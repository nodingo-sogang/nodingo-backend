package nodingo.core.batch.service.query;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nodingo.core.ai.dto.newsBatch.NewsBatch;
import nodingo.core.news.domain.News;
import nodingo.core.news.repository.NewsRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NewsBatchQueryService {
    private final NewsRepository newsRepository;

    public NewsBatch.Request getRealNewsRequestJson() {
        List<News> realNewsList = newsRepository.findAll(PageRequest.of(0, 100)).getContent();
        log.info(">>>> [News Batch Query] Loaded {} news articles from DB.", realNewsList.size());

        List<NewsBatch.NewsInput> newsInputs = realNewsList.stream()
                .map(n -> NewsBatch.NewsInput.builder()
                        .newsId(n.getId())
                        .title(n.getTitle())
                        .body(n.getBody())
                        .build())
                .toList();

        return NewsBatch.Request.builder()
                .news(newsInputs)
                .existingKeywords(new ArrayList<>())
                .topKKeywords(5)
                .build();
    }
}