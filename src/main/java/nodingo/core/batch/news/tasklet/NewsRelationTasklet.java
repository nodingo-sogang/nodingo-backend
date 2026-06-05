package nodingo.core.batch.news.tasklet;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nodingo.core.ai.client.AiClient;
import nodingo.core.ai.dto.relation.NewsRelationAnalysis;
import nodingo.core.global.util.BatchDateUtil;
import nodingo.core.news.domain.News;
import nodingo.core.news.domain.NewsRelation;
import nodingo.core.news.repository.NewsRelationRepository;
import nodingo.core.news.repository.NewsRepository;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class NewsRelationTasklet implements Tasklet {

    private final NewsRepository newsRepository;
    private final NewsRelationRepository newsRelationRepository;
    private final AiClient aiClient;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        log.info(">>>> [Relation Tasklet] Starting news relation building process.");

        LocalDate targetDate = BatchDateUtil.getTargetDate();
        LocalDateTime startTime = targetDate.atTime(5, 0, 0);
        LocalDateTime endTime = targetDate.plusDays(1).atTime(4, 59, 59);

        List<News> targetNews = newsRepository.findAllByDateTimePubBetweenAndEmbeddingIsNotNull(startTime, endTime);

        if (targetNews.size() < 2) {
            log.warn(">>>> [Relation Tasklet] Not enough news to compare today. (count: {})", targetNews.size());
            return RepeatStatus.FINISHED;
        }

        Map<Long, News> newsMap = targetNews.stream()
                .collect(Collectors.toMap(News::getId, Function.identity()));

        List<NewsRelationAnalysis.NewsEmbeddingInput> inputs = targetNews.stream()
                .map(n -> NewsRelationAnalysis.NewsEmbeddingInput.builder()
                        .newsId(n.getId())
                        .embedding(n.getEmbedding())
                        .build())
                .toList();

        NewsRelationAnalysis.Request request = NewsRelationAnalysis.Request.builder()
                .newsEmbeddings(inputs)
                .similarityThreshold(0.55)
                .topK(5)
                .build();

        try {
            NewsRelationAnalysis.Response response = aiClient.buildNewsRelations(request);

            if (response.getRelations() != null) {
                List<NewsRelation> relationsToSave = new ArrayList<>();

                for (NewsRelationAnalysis.RelationResult res : response.getRelations()) {
                    if (res.getSubjectNewsId().equals(res.getRelatedNewsId())) {
                        log.warn(">>>> [Relation Tasklet] SKIP: AI made news connected itself.(News ID: {})", res.getSubjectNewsId());
                        continue;
                    }
                    News subject = newsMap.get(res.getSubjectNewsId());
                    News related = newsMap.get(res.getRelatedNewsId());

                    if (subject != null && related != null) {
                        NewsRelation relation = NewsRelation.create(subject, related, res.getRelationScore());
                        relationsToSave.add(relation);
                    }
                }

                if (!relationsToSave.isEmpty()) {
                    newsRelationRepository.saveAll(relationsToSave);
                    log.info(">>>> [Relation Tasklet] Successfully saved {} relations to the DB.", relationsToSave.size());
                }
            }

        } catch (Exception e) {
            log.error(">>>> [Relation Tasklet] AI server communication error: {}", e.getMessage(), e);
        }

        return RepeatStatus.FINISHED;
    }
}