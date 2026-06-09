package nodingo.core.batch.news.writer;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nodingo.core.ai.client.AiClient;
import nodingo.core.ai.dto.newsBatch.NewsBatch;
import nodingo.core.global.exception.ai.AiRateLimitException;
import nodingo.core.global.metrics.MonitoringMetrics;
import nodingo.core.global.util.DateUtil;
import nodingo.core.keyword.domain.Keyword;
import nodingo.core.keyword.domain.KeywordRelation;
import nodingo.core.keyword.repository.KeywordRelationRepository;
import nodingo.core.keyword.repository.KeywordRepository;
import nodingo.core.news.domain.News;
import nodingo.core.news.repository.NewsRepository;
import nodingo.core.user.domain.InterestLevel;
import nodingo.core.user.domain.UserPersona;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;

import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class NewsAiWriter implements ItemWriter<News> {
    private final EntityManager entityManager;
    private final NewsRepository newsRepository;
    private final KeywordRepository keywordRepository;
    private final KeywordRelationRepository keywordRelationRepository;
    private final AiClient aiClient;
    private final MonitoringMetrics metrics;

    private static final int TOP_K_KEYWORDS = 5;

    @Override
    public void write(Chunk<? extends News> items) {
        List<News> chunkItems = new ArrayList<>(items.getItems());
        if (chunkItems.isEmpty()) return;

        LocalDate targetDate = DateUtil.getTargetDate();

        List<News> savedNews = newsRepository.saveAll(chunkItems);
        Map<Long, News> newsMap = savedNews.stream()
                .collect(Collectors.toMap(News::getId, Function.identity()));

        List<Keyword> existingKeywords = keywordRepository.findAllByTargetDate(targetDate);

        NewsBatch.Request request = NewsBatch.Request.builder()
                .news(savedNews.stream()
                        .map(n -> NewsBatch.NewsInput.builder()
                                .newsId(n.getId())
                                .title(n.getTitle())
                                .body(n.getBody())
                                .build())
                        .toList())
                .existingKeywords(existingKeywords.stream()
                        .map(k -> NewsBatch.ExistingKeywordInput.builder()
                                .keywordId(k.getId())
                                .word(k.getWord())
                                .normalizedWord(k.getNormalizedWord())
                                .embedding(k.getEmbedding())
                                .build())
                        .toList())
                .topKKeywords(TOP_K_KEYWORDS)
                .build();

        log.info(">>>> [Batch Writer] Requesting AI analysis for {} news articles. (Chunk size: {})",
                savedNews.size(), items.size());

        NewsBatch.Response aiResponse;
        try {
            metrics.recordAiCall("batch.analyzeNewsBatch");
            aiResponse = aiClient.analyzeNewsBatch(request);
        } catch (AiRateLimitException e) {
            metrics.recordAiFailure("batch.analyzeNewsBatch", "RateLimitError");
            log.error(">>>> [Batch Writer] OpenAI rate limit exceeded (429). chunkSize={}", items.size(), e);
            throw e;
        } catch (Exception e) {
            metrics.recordAiFailure("batch.analyzeNewsBatch", e.getClass().getSimpleName());
            log.error(">>>> [Batch Writer] AI analysis failed. chunkSize={}", items.size(), e);
            throw e;
        }

        for (NewsBatch.NewsAnalysisResult res : aiResponse.getNewsResults()) {
            News news = newsMap.get(res.getNewsId());
            if (news != null) {
                news.updateEmbedding(res.getEmbedding());
                news.updateBody(res.getSummary());
            }
        }

        Set<String> allNormWords = new HashSet<>();
        Set<String> allMacroNames = new HashSet<>();

        for (NewsBatch.NewsAnalysisResult res : aiResponse.getNewsResults()) {
            for (NewsBatch.KeywordAiResult kwRes : res.getKeywords()) {
                if (kwRes.getNormalizedWord() != null) allNormWords.add(kwRes.getNormalizedWord());
                if (kwRes.getMacro() != null && !kwRes.getMacro().isBlank()) allMacroNames.add(kwRes.getMacro());
            }
        }

        Map<String, Keyword> specificCache = keywordRepository.findSpecificsByDate(allNormWords, targetDate)
                .stream()
                .collect(Collectors.toMap(Keyword::getNormalizedWord, k -> k, (a, b) -> a));

        Map<String, Keyword> macroCache = keywordRepository.findMacrosByDate(allMacroNames, targetDate)
                .stream()
                .collect(Collectors.toMap(Keyword::getWord, k -> k, (a, b) -> a));

        for (NewsBatch.NewsAnalysisResult res : aiResponse.getNewsResults()) {
            News news = newsMap.get(res.getNewsId());
            if (news == null) continue;

            for (NewsBatch.KeywordAiResult kwRes : res.getKeywords()) {
                String normWord = kwRes.getNormalizedWord();
                String macroName = kwRes.getMacro();
                String personaStr = kwRes.getPersonas();

                UserPersona persona = null;
                if (personaStr != null && !personaStr.isBlank()) {
                    try {
                        persona = UserPersona.valueOf(personaStr.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        log.warn(">>>> [Batch Writer] Unknown persona received from AI: persona={}, newsId={}",
                                personaStr, res.getNewsId());
                    }
                }

                Keyword macroKeyword = null;
                if (macroName != null && !macroName.isBlank() && persona != null) {
                    final UserPersona finalPersona = persona;
                    final LocalDate finalTargetDate = targetDate;
                    macroKeyword = macroCache.computeIfAbsent(macroName, name -> {
                        return keywordRepository.findByWordAndLevelAndTargetDate(name, InterestLevel.MACRO, finalTargetDate)
                                .orElseGet(() -> {
                                    Keyword newMacro = Keyword.createMacro(name, finalPersona, finalTargetDate);
                                    return keywordRepository.save(newMacro);
                                });
                    });
                }

                final Keyword finalMacroKeyword = macroKeyword;
                final UserPersona finalSpecificPersona = persona;
                final LocalDate finalTargetDate = targetDate;

                Keyword keyword = specificCache.computeIfAbsent(normWord, key -> {
                    return keywordRepository.findByNormalizedWordAndLevelAndTargetDate(normWord, InterestLevel.SPECIFIC, finalTargetDate)
                            .orElseGet(() -> {
                                Keyword newKw;
                                if (finalMacroKeyword != null && finalSpecificPersona != null) {
                                    newKw = Keyword.createSpecific(kwRes.getWord(), finalSpecificPersona, finalMacroKeyword, finalTargetDate);
                                } else {
                                    newKw = Keyword.create(kwRes.getWord(), finalTargetDate);
                                }
                                newKw.updateEmbedding(kwRes.getEmbedding());
                                return keywordRepository.save(newKw);
                            });
                });

                news.addKeyword(keyword, kwRes.getWeight());
            }
        }

        newsRepository.saveAll(savedNews);
        log.info(">>>> [Batch Writer] Finished analyzing and summarizing {} news articles.", savedNews.size());

        if (aiResponse.getKeywordRelations() != null && !aiResponse.getKeywordRelations().isEmpty()) {
            int skippedNullNormalizedCount = 0;
            int skippedKeywordNotFoundCount = 0;
            int duplicateRelationCount = 0;
            List<KeywordRelation> relationsToSave = new ArrayList<>();

            for (NewsBatch.KeywordRelationResult relRes : aiResponse.getKeywordRelations()) {
                String subjectNorm = relRes.getSubjectNormalizedWord();
                String relatedNorm = relRes.getRelatedNormalizedWord();

                if (subjectNorm == null || relatedNorm == null) {
                    skippedNullNormalizedCount++;
                    continue;
                }

                Keyword source = specificCache.get(subjectNorm);
                Keyword target = specificCache.get(relatedNorm);

                if (source == null || target == null) {
                    skippedKeywordNotFoundCount++;
                    continue;
                }

                if (source.getId().equals(target.getId())) {
                    log.warn(">>>> [Batch Writer] Skipping self-relation. keywordId={}", source.getId());
                    continue;
                }

                Long subjectId = source.getId() < target.getId() ? source.getId() : target.getId();
                Long relatedId = source.getId() < target.getId() ? target.getId() : source.getId();

                Optional<KeywordRelation> existing =
                        keywordRelationRepository.findByPair(subjectId, relatedId);

                if (existing.isPresent()) {
                    existing.get().updateRelation(relRes.getRelationScore());
                    keywordRelationRepository.save(existing.get());
                    duplicateRelationCount++;
                } else {
                    relationsToSave.add(KeywordRelation.create(source, target, relRes.getRelationScore()));
                }
            }

            if (!relationsToSave.isEmpty()) {
                keywordRelationRepository.saveAll(relationsToSave);
            }

            log.info(">>>> [Batch Writer] KeywordRelation stats | AI total: {} | toSave: {} | skippedNullNorm: {} | skippedNotFound: {} | duplicateUpdated: {}",
                    aiResponse.getKeywordRelations().size(),
                    relationsToSave.size(),
                    skippedNullNormalizedCount,
                    skippedKeywordNotFoundCount,
                    duplicateRelationCount);
        }
        entityManager.flush();
        entityManager.clear();
    }
}