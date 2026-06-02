package nodingo.core.batch.news.reader;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nodingo.core.batch.dto.article.NewsApiItem;
import nodingo.core.batch.dto.article.NewsApiResponse;
import nodingo.core.batch.service.query.NewsFetchService;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;


import java.time.LocalDate;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class NewsApiReader implements ItemReader<NewsApiItem> {

    private final NewsFetchService newsFetchService;

    private static final int MAX_TEST_PAGES = 5;
    private int currentPage = 1;
    private Iterator<NewsApiItem> itemIterator = Collections.emptyIterator();
    private boolean isEnd = false;

    @Override
    public NewsApiItem read() {
        if (itemIterator.hasNext()) {
            return itemIterator.next();
        }

        if (isEnd || currentPage > MAX_TEST_PAGES) {
            log.info(">>>> [Batch Reader] Finished or reached max test pages.");
            return null;
        }

        LocalDate targetDate = LocalDate.now().minusDays(1);

        NewsApiResponse response = newsFetchService.fetchNews(targetDate, currentPage);

        if (response == null || response.getArticles() == null ||
                response.getArticles().getResults() == null || response.getArticles().getResults().isEmpty()) {
            log.warn(">>>> [Batch Reader] API response is empty. date: {}, page: {}", targetDate, currentPage);
            isEnd = true;
            return null;
        }

        // 5. Iterator 갱신 및 페이지 정보 계산
        List<NewsApiItem> results = response.getArticles().getResults();
        itemIterator = results.iterator();

        int totalPages = response.getArticles().getPages() > 0 ? response.getArticles().getPages() : currentPage;
        int effectiveTotalPages = Math.min(totalPages, MAX_TEST_PAGES);

        log.info(">>>> [Batch Reader] Fetching Article page: {} / {} (total={}, items={})",
                currentPage, effectiveTotalPages, totalPages, results.size());

        if (currentPage >= effectiveTotalPages) {
            isEnd = true;
        }

        currentPage++;

        // 6. 새로 읽어온 데이터의 첫 번째 아이템 반환
        return itemIterator.hasNext() ? itemIterator.next() : null;
    }
}