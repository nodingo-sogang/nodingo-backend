package nodingo.core.ai.dto.keyword;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

public class KeywordRecommend {

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class Request {
        @JsonProperty("user_id")
        private Long userId;
        @JsonProperty("user_embedding")
        private float[] userEmbedding;
        @JsonProperty("candidate_keywords")
        private List<CandidateKeyword> candidateKeywords;
        @JsonProperty("target_date")
        private LocalDate targetDate;
        @JsonProperty("top_k")
        private int topK;
    }

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class Response {
        @JsonProperty("recommend_keywords")
        private List<RecommendResult> recommendKeywords;
    }

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class CandidateKeyword {
        @JsonProperty("keyword_id")
        private Long keywordId;
        @JsonProperty("word")
        private String word;
        @JsonProperty("normalized_word")
        private String normalizedWord;
        @JsonProperty("embedding")
        private float[] embedding;
        @JsonProperty("recent_importance")
        private double recentImportance;
        @JsonProperty("is_user_interest")
        private boolean isUserInterest;
    }

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class RecommendResult {
        @JsonProperty("user_id")
        private Long userId;
        @JsonProperty("keyword_id")
        private Long keywordId;
        @JsonProperty("target_date")
        private LocalDate targetDate;
        @JsonProperty("score")
        private double score;
        @JsonProperty("summary")
        private String summary;
    }
}