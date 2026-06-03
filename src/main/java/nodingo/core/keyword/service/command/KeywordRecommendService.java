package nodingo.core.keyword.service.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nodingo.core.ai.client.AiClient;
import nodingo.core.ai.dto.keyword.KeywordRecommend;
import nodingo.core.keyword.domain.RecommendKeyword;
import nodingo.core.user.repository.UserInterestRepository;
import nodingo.core.keyword.repository.KeywordRepository;
import nodingo.core.keyword.repository.RecommendKeywordRepository;
import nodingo.core.user.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional

public class KeywordRecommendService {
    public static final int TOP_K_RECOMMENDATIONS = 12;

    private final AiClient aiClient;
    private final KeywordRepository keywordRepository;
    private final RecommendKeywordRepository recommendKeywordRepository;
    private final UserInterestRepository userInterestRepository;

    public List<RecommendKeyword> generateRecommendationForUser(
            User user,
            List<KeywordRecommend.CandidateKeyword> commonCandidateKeywords,
            LocalDate targetDate) {

        if (user.getEmbedding() == null || commonCandidateKeywords.isEmpty()) {
            return List.of();
        }

        Set<Long> userInterestKeywordIds = userInterestRepository.findByUserId(user.getId()).stream()
                .map(interest -> interest.getKeyword().getId())
                .collect(Collectors.toSet());

        List<KeywordRecommend.CandidateKeyword> personalizedCandidates = commonCandidateKeywords.stream()
                .map(candidate -> KeywordRecommend.CandidateKeyword.builder()
                        .keywordId(candidate.getKeywordId())
                        .word(candidate.getWord())
                        .normalizedWord(candidate.getNormalizedWord())
                        .embedding(candidate.getEmbedding())
                        .recentImportance(candidate.getRecentImportance())
                        .isUserInterest(userInterestKeywordIds.contains(candidate.getKeywordId())) // 🔥 여기서 매핑!
                        .build())
                .collect(Collectors.toList());

        KeywordRecommend.Request request = KeywordRecommend.Request.builder()
                .userId(user.getId())
                .userEmbedding(user.getEmbedding())
                .candidateKeywords(personalizedCandidates)
                .targetDate(targetDate)
                .topK(TOP_K_RECOMMENDATIONS)
                .build();

        KeywordRecommend.Response response = aiClient.recommendKeywords(request);

        recommendKeywordRepository.deleteByUserIdAndTargetDate(user.getId(), targetDate);

        List<RecommendKeyword> recommendEntities = response.getRecommendKeywords().stream()
                .map(res -> RecommendKeyword.create(
                        user,
                        keywordRepository.getReferenceById(res.getKeywordId()),
                        targetDate,
                        res.getScore()
                ))
                .collect(Collectors.toList());

        return recommendKeywordRepository.saveAll(recommendEntities);
    }
}
