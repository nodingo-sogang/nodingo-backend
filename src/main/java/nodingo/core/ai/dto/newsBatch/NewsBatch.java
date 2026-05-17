package nodingo.core.ai.dto.newsBatch;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

public class NewsBatch {

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class Request {
        private List<NewsInput> news;
        @JsonProperty("existing_keywords")
        private List<ExistingKeywordInput> existingKeywords;
        @JsonProperty("top_k_keywords")
        private int topKKeywords;
    }

    @Getter
    @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class Response {
        private List<NewsAnalysisResult> newsResults;
        private List<KeywordRelationResult> keywordRelations;

    }

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class NewsInput {
        private Long newsId;
        private String title;
        private String body;
    }

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class ExistingKeywordInput {
        private Long keywordId;
        private String word;
        private String normalizedWord;
        private float[] embedding;
    }

    @Getter
    @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class NewsAnalysisResult {
        private Long newsId;
        private float[] embedding;
        private String summary;
        private List<KeywordAiResult> keywords;

    }

    @Getter
    @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class KeywordAiResult {
        private Long keywordId;
        private String word;
        private String normalizedWord;
        private float[] embedding;
        private double weight;
        private boolean isNew;
        private List<String> aliases;

        private String personas;
        private String macro;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KeywordRelationResult {
        private Long sourceKeywordId;
        private Long targetKeywordId;
        private double relationScore;
    }
}