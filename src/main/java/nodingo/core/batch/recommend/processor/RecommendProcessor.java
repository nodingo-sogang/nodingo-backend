package nodingo.core.batch.recommend.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nodingo.core.ai.dto.keyword.KeywordRecommend;
import nodingo.core.global.util.DateUtil;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendProcessor {

    private final KeywordRecommendQueryService queryService;
    private final KeywordRecommendService commandService;

    @Bean
    @StepScope
    public ItemProcessor<User, List<RecommendKeyword>> recommendItemProcessor() {
        LocalDate targetDate = DateUtil.getTargetDate();
        List<KeywordRecommend.CandidateKeyword> allCommonCandidates = queryService.getDailyCandidateKeywords(targetDate);
        log.info(">>>> [Recommend Processor] Initialized. targetDate={}, candidates={}",
                targetDate, allCommonCandidates.size());
        return user -> commandService.generateRecommendationForUser(user, allCommonCandidates, targetDate);
    }
}