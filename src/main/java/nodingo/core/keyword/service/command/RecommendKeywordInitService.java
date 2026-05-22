package nodingo.core.keyword.service.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nodingo.core.ai.client.AiClient;
import nodingo.core.ai.dto.keyword.KeywordRecommend;
import nodingo.core.ai.dto.keyword.KeywordSummary;
import nodingo.core.keyword.domain.NewsKeyword;
import nodingo.core.keyword.domain.RecommendKeyword;
import nodingo.core.keyword.repository.NewsKeywordRepository;
import nodingo.core.keyword.repository.RecommendKeywordRepository;
import nodingo.core.keyword.service.query.KeywordRecommendQueryService;
import nodingo.core.user.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendKeywordInitService {

    private final KeywordRecommendQueryService queryService;
    private final KeywordRecommendService commandService;
    private final RecommendKeywordRepository recommendKeywordRepository;
    private final NewsKeywordRepository newsKeywordRepository;  // 🔥 추가
    private final AiClient aiClient;  // 🔥 추가

    @Transactional
    public void initForNewUser(User user) {
        LocalDate today = LocalDate.now(); // 🔥 그냥 오늘

        if (recommendKeywordRepository.existsByUserIdAndTargetDate(user.getId(), today)) {
            log.info(">>>> [Onboarding] Recommend keywords already exist, skipping. userId={}", user.getId());
            return;
        }

        List<KeywordRecommend.CandidateKeyword> candidates = queryService.getDailyCandidateKeywords(today);

        if (candidates.isEmpty()) {
            log.warn(">>>> [Onboarding] No candidate keywords found for today. userId={}", user.getId());
            return;
        }

        // 1. 추천 키워드 생성
        List<RecommendKeyword> recommendations = commandService.generateRecommendationForUser(user, candidates, today);

        // 2. 🔥 각 키워드 summarize
        for (RecommendKeyword rk : recommendations) {
            try {
                List<NewsKeyword> topNewsKeywords = newsKeywordRepository.findTopByKeywordId(
                        rk.getKeyword().getId(), 3);

                if (topNewsKeywords.isEmpty()) {
                    rk.updateSummary("관련 뉴스가 부족하여 요약할 수 없습니다.");
                    continue;
                }

                List<KeywordSummary.SummaryNewsInput> newsInputs = topNewsKeywords.stream()
                        .map(nk -> KeywordSummary.SummaryNewsInput.builder()
                                .newsId(nk.getNews().getId())
                                .title(nk.getNews().getTitle())
                                .body(nk.getNews().getBody())
                                .build())
                        .collect(Collectors.toList());

                KeywordSummary.Request aiRequest = KeywordSummary.Request.builder()
                        .userId(user.getId())
                        .keyword(KeywordSummary.SummaryKeywordInput.builder()
                                .keywordId(rk.getKeyword().getId())
                                .word(rk.getKeyword().getWord())
                                .build())
                        .relatedNews(newsInputs)
                        .relatedKeywords(Collections.emptyList())
                        .targetDate(today)
                        .build();

                KeywordSummary.Response aiResponse = aiClient.summarizeKeywords(aiRequest);
                rk.updateSummary(aiResponse.getSummary());

                log.info(">>>> [Onboarding] Summary created for keyword: {}", rk.getKeyword().getWord());

            } catch (Exception e) {
                log.warn(">>>> [Onboarding] Summary failed for keyword: {}, error: {}",
                        rk.getKeyword().getWord(), e.getMessage());
            }
        }

        recommendKeywordRepository.saveAll(recommendations);

        log.info(">>>> [Onboarding] Successfully initialized recommend keywords for new user. userId={}, count={}",
                user.getId(), recommendations.size());
    }
}
