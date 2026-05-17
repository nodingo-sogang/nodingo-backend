package nodingo.core.batch.news.writer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nodingo.core.ai.client.AiClient;
import nodingo.core.ai.dto.newsBatch.NewsBatch;
import nodingo.core.keyword.domain.Keyword;
import nodingo.core.keyword.domain.KeywordRelation;
import nodingo.core.keyword.repository.KeywordRelationRepository;
import nodingo.core.keyword.repository.KeywordRepository;
import nodingo.core.keyword.service.query.KeywordQueryService;
import nodingo.core.news.domain.News;
import nodingo.core.news.repository.NewsRepository;
import nodingo.core.user.domain.InterestLevel;
import nodingo.core.user.domain.UserPersona;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.*;

import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class NewsAiWriter implements ItemWriter<News> {

    private final NewsRepository newsRepository;
    private final KeywordRepository keywordRepository;
    private final KeywordRelationRepository keywordRelationRepository;
    private final AiClient aiClient;
    private final KeywordQueryService keywordQueryService;

    private static final int TOP_K_KEYWORDS = 5;

    @Override
    public void write(Chunk<? extends News> items) {
        List<News> chunkItems = new ArrayList<>(items.getItems());

        if (chunkItems.isEmpty()) return;

        // 1. DB 1차 저장 (AI 서버에 보낼 ID 확보)
        List<News> savedNews = newsRepository.saveAll(chunkItems);

        Map<Long, News> newsMap = savedNews.stream()
                .collect(Collectors.toMap(News::getId, Function.identity()));

        // 2. 파이썬 전송용 기존 키워드 목록 비우기
        List<NewsBatch.ExistingKeywordInput> existingKeywords = new ArrayList<>();

        // 3. AI 분석 요청 객체 생성
        NewsBatch.Request request = NewsBatch.Request.builder()
                .news(savedNews.stream()
                        .map(n -> NewsBatch.NewsInput.builder()
                                .newsId(n.getId())
                                .title(n.getTitle())
                                .body(n.getBody())
                                .build())
                        .toList())
                .existingKeywords(existingKeywords)
                .topKKeywords(TOP_K_KEYWORDS)
                .build();

        log.info(">>>> [Batch Writer] Requesting AI analysis for {} news articles. (Chunk size: {})",
                savedNews.size(), items.size());

        // 4. FastAPI 서버 호출
        NewsBatch.Response aiResponse = aiClient.analyzeNewsBatch(request);

        // 5. AI 분석 결과 반영 (임베딩 및 요약본 업데이트)
        for (NewsBatch.NewsAnalysisResult res : aiResponse.getNewsResults()) {
            News news = newsMap.get(res.getNewsId());
            if (news != null) {
                news.updateEmbedding(res.getEmbedding());
                news.updateBody(res.getSummary()); // 요약본 반영
            }
        }

        // 6. 이번 Chunk에서 나온 소분류(Specific) 키워드 추출
        Set<String> extractedWords = aiResponse.getNewsResults().stream()
                .flatMap(res -> res.getKeywords().stream())
                .map(NewsBatch.KeywordAiResult::getNormalizedWord)
                .collect(Collectors.toSet());

        // 7. 기존 키워드 Map 로드 (소분류 전용)
        Map<String, Keyword> existingKeywordMap = keywordQueryService.getExistingKeywordsMap(extractedWords);

        // 🌟 [신규 추가] N+1 쿼리 방지용 중분류(Macro) 메모리 캐시
        Map<String, Keyword> macroCache = new HashMap<>();

        // 8. 키워드 계층 매핑 및 신규 저장
        for (NewsBatch.NewsAnalysisResult res : aiResponse.getNewsResults()) {
            News news = newsMap.get(res.getNewsId());
            if (news == null) continue;

            for (NewsBatch.KeywordAiResult kwRes : res.getKeywords()) {
                String normWord = kwRes.getNormalizedWord();
                String macroName = kwRes.getMacro();
                String personaStr = kwRes.getPersonas();

                // 🌟 A. String으로 온 페르소나를 Enum(UserPersona)으로 안전하게 변환
                UserPersona persona = null;
                if (personaStr != null && !personaStr.isBlank()) {
                    try {
                        persona = UserPersona.valueOf(personaStr.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        log.warn("Unknown persona received from AI: {}", personaStr);
                    }
                }

                // 🌟 B. 중분류(Macro) 탐색 및 생성 (DB에 없으면 생성 후 캐시에 넣음)
                Keyword macroKeyword = null;
                if (macroName != null && !macroName.isBlank() && persona != null) {
                    final UserPersona finalPersona = persona;
                    macroKeyword = macroCache.computeIfAbsent(macroName, name ->
                            keywordRepository.findByWordAndLevel(name, InterestLevel.MACRO)
                                    .orElseGet(() -> {
                                        Keyword newMacro = Keyword.createOnboardingKeyword(name, finalPersona, InterestLevel.MACRO, null);
                                        return keywordRepository.save(newMacro);
                                    })
                    );
                }

                // 🌟 C. 소분류(Specific) 탐색 및 생성 (중분류 부모 꽂아주기)
                final Keyword finalMacroKeyword = macroKeyword;
                final UserPersona finalSpecificPersona = persona;

                Keyword keyword = existingKeywordMap.computeIfAbsent(normWord, key -> {
                    Keyword newKw;
                    if (finalMacroKeyword != null && finalSpecificPersona != null) {
                        // 정상 케이스: 중분류 부모가 존재하는 계층형 소분류 생성
                        newKw = Keyword.createOnboardingKeyword(kwRes.getWord(), finalSpecificPersona, InterestLevel.SPECIFIC, finalMacroKeyword);
                    } else {
                        // 예외 케이스: 파이썬에서 분류를 못 해준 경우 기존 평면(Flat) 방식으로 생성
                        newKw = Keyword.create(kwRes.getWord());
                    }
                    newKw.updateEmbedding(kwRes.getEmbedding());
                    return keywordRepository.save(newKw); // 진짜 새로운 녀석만 INSERT
                });

                news.addKeyword(keyword, kwRes.getWeight());
            }
        }

        // 9. 뉴스 최종 업데이트
        newsRepository.saveAll(savedNews);
        log.info(">>>> [Batch Writer] Finished analyzing and summarizing {} news articles.", savedNews.size());

        // 10. 키워드 관계 저장
        if (aiResponse.getKeywordRelations() != null && !aiResponse.getKeywordRelations().isEmpty()) {
            List<KeywordRelation> relationsToSave = new ArrayList<>();

            for (NewsBatch.KeywordRelationResult relRes : aiResponse.getKeywordRelations()) {
                if (relRes.getSourceKeywordId() != null && relRes.getTargetKeywordId() != null) {
                    Keyword source = keywordRepository.findById(relRes.getSourceKeywordId()).orElse(null);
                    Keyword target = keywordRepository.findById(relRes.getTargetKeywordId()).orElse(null);

                    if (source != null && target != null) {
                        relationsToSave.add(KeywordRelation.create(source, target, relRes.getRelationScore()));
                    }
                }
            }

            if (!relationsToSave.isEmpty()) {
                keywordRelationRepository.saveAll(relationsToSave);
                log.info(">>>> [Batch Writer] Successfully saved {} keyword relations.", relationsToSave.size());
            }
        }
    }
}