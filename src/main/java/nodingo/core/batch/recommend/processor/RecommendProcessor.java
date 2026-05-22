package nodingo.core.batch.recommend.processor;

import lombok.RequiredArgsConstructor;
import nodingo.core.ai.dto.keyword.KeywordRecommend;
import nodingo.core.keyword.domain.RecommendKeyword;
import nodingo.core.keyword.service.command.KeywordRecommendService;
import nodingo.core.keyword.service.query.KeywordRecommendQueryService;
import nodingo.core.user.domain.User;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;


import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RecommendProcessor {

    private final KeywordRecommendQueryService queryService;
    private final KeywordRecommendService commandService;

    @Bean
    @StepScope
    public ItemProcessor<User, List<RecommendKeyword>> recommendItemProcessor() {
        LocalDate targetDate = LocalDate.now();
        List<KeywordRecommend.CandidateKeyword> allCommonCandidates = queryService.getDailyCandidateKeywords(targetDate);
        return user -> commandService.generateRecommendationForUser(user, allCommonCandidates, targetDate);
    }
}