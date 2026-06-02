package nodingo.core.ai.dto.keyword;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

public class KeywordSummary {

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class Request {
        @JsonProperty("keyword")
        private SummaryKeywordInput keyword;
        @JsonProperty("related_news")
        private List<SummaryNewsInput> relatedNews;
        @JsonProperty("related_keywords")
        private List<SummaryRelatedKeywordInput> relatedKeywords;
        @JsonProperty("target_date")
        private LocalDate targetDate;
        @JsonProperty("persona")
        private String persona;
        @JsonProperty("category")
        private String category;
    }

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class Response {
        @JsonProperty("keyword_id")
        private Long keywordId;
        @JsonProperty("target_date")
        private LocalDate targetDate;
        @JsonProperty("summary")
        private String summary;
    }

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class SummaryKeywordInput {
        @JsonProperty("keyword_id")
        private Long keywordId;
        @JsonProperty("word")
        private String word;
    }

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class SummaryNewsInput {
        @JsonProperty("news_id")
        private Long newsId;
        @JsonProperty("title")
        private String title;
        @JsonProperty("body")
        private String body;
    }

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class SummaryRelatedKeywordInput {
        @JsonProperty("keyword_id")
        private Long keywordId;
        @JsonProperty("word")
        private String word;
    }
}