package nodingo.core.ai.dto.userEmbedding;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nodingo.core.keyword.domain.Keyword;
import nodingo.core.news.domain.News;

import java.util.List;

public class UserEmbedding {

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class InitRequest {
        @JsonProperty("user_id")
        private Long userId;
        @JsonProperty("interest_keywords")
        private List<InterestKeyword> interestKeywords;
    }

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class UpdateRequest {
        @JsonProperty("user_id")
        private Long userId;
        @JsonProperty("old_embedding")
        private float[] oldEmbedding;
        @JsonProperty("activities")
        private List<Activity> activities;
        @JsonProperty("decay")
        private double decay;
    }

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class Response {
        @JsonProperty("user_id")
        private Long userId;
        @JsonProperty("embedding")
        private float[] embedding;
    }

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class InterestKeyword {
        @JsonProperty("keyword_id")
        private Long keywordId;
        @JsonProperty("word")
        private String word;
        @JsonProperty("embedding")
        private float[] embedding;
    }

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class Activity {
        @JsonProperty("type")
        private String type;
        @JsonProperty("news_id")
        private Long newsId;
        @JsonProperty("news_embedding")
        private float[] newsEmbedding;
        @JsonProperty("keyword_id")
        private Long keywordId;
        @JsonProperty("keyword_embedding")
        private float[] keywordEmbedding;
        @JsonProperty("weight")
        private double weight;

        public static Activity createNewsScrap(News news, double weight) {
            return Activity.builder()
                    .type("SCRAP")
                    .newsId(news.getId())
                    .newsEmbedding(news.getEmbedding())
                    .weight(weight)
                    .build();
        }

        public static Activity createKeywordActivity(Keyword keyword, double weight) {
            return Activity.builder()
                    .type("CLICK")
                    .keywordId(keyword.getId())
                    .keywordEmbedding(keyword.getEmbedding())
                    .weight(weight)
                    .build();
        }
    }
}