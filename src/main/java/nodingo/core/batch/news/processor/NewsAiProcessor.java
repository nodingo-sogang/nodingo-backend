package nodingo.core.batch.news.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nodingo.core.batch.dto.article.NewsApiItem;
import nodingo.core.news.domain.News;
import nodingo.core.news.repository.NewsRepository;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class NewsAiProcessor implements ItemProcessor<NewsApiItem, News> {

    private final NewsRepository newsRepository;

    @Override
    public News process(NewsApiItem articleItem) {
        if (articleItem == null) return null;

        String articleUri = articleItem.getUri();

        if (articleUri == null || articleUri.isBlank()) {
            log.warn(">>>> [Batch Processor] Skip: uri is empty. url={}", articleItem.getUrl());
            return null;
        }
        if (newsRepository.existsByUri(articleUri)) {
            log.debug(">>>> [Batch Processor] Skip: already exists. uri={}", articleUri);
            return null;
        }
        if (articleItem.getBody() == null || articleItem.getBody().isBlank()) {
            log.warn(">>>> [Batch Processor] Skip: body is empty. uri={}", articleUri);
            return null;
        }

        return NewsApiItem.toEntity(articleItem);
    }
}