package nodingo.core.batch.service.query;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nodingo.core.batch.dto.article.NewsApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsFetchService {

    private final RestClient newsApiClient;

    @Value("${news.api.key}")
    private String apiKey;

    public NewsApiResponse fetchNews(LocalDate targetDate, int page) {
        log.info(">>>> [NewsFetchService] Article API request - targetDate: {}, page: {}", targetDate, page);

        try {
            NewsApiResponse response = newsApiClient.post()
                    .uri("/article/getArticles")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(createRequestBody(targetDate, page))
                    .retrieve()
                    .body(NewsApiResponse.class);

            if (response == null || response.getArticles() == null) {
                throw new IllegalStateException("Article API response body is null");
            }

            log.info(">>>> [NewsFetchService] Article API response - articles={}, pages={}",
                    response.getArticles().getResults() != null ? response.getArticles().getResults().size() : 0,
                    response.getArticles().getPages());
            return response;

        } catch (Exception e) {
            log.error(">>>> [NewsFetchService] Error occurred during API request: {}", e.getMessage(), e);
            throw new RuntimeException("News API 통신 및 매핑 실패", e);
        }
    }

    private Map<String, Object> createRequestBody(LocalDate targetDate, int page) {
        Map<String, Object> body = new HashMap<>();
        body.put("action", "getArticles");

        LocalDateTime startDateTime = targetDate.atTime(5, 0, 0);
        LocalDateTime endDateTime = targetDate.plusDays(1).atTime(4, 59, 59);

        body.put("dateStart", startDateTime.toLocalDate().toString());
        body.put("dateEnd", endDateTime.toLocalDate().toString());

        body.put("timeStart", startDateTime.toLocalTime().truncatedTo(ChronoUnit.SECONDS).toString());
        body.put("timeEnd", endDateTime.toLocalTime().truncatedTo(ChronoUnit.SECONDS).toString());

        body.put("lang", "kor");

        body.put("articlesPage", page);
        body.put("articlesCount", 100);
        body.put("articlesSortBy", "date");
        body.put("resultType", "articles");

        body.put("articleBodyLen", -1);

        body.put("apiKey", apiKey);

        log.info(">>>> [NewsFetchService] Fetch Range: {} {} ~ {} {}",
                body.get("dateStart"), body.get("timeStart"),
                body.get("dateEnd"), body.get("timeEnd"));

        return body;
    }
}