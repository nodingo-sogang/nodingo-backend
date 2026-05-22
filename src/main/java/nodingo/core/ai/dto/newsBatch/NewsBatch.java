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
        @JsonProperty("news")
        private List<NewsInput> news;
        @JsonProperty("existing_keywords")
        private List<ExistingKeywordInput> existingKeywords;
        @JsonProperty("top_k_keywords")
        private int topKKeywords;
    }

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class Response {
        @JsonProperty("news_results")
        private List<NewsAnalysisResult> newsResults;
        @JsonProperty("keyword_relations")
        private List<KeywordRelationResult> keywordRelations;
    }

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class NewsInput {
        @JsonProperty("news_id")
        private Long newsId;
        @JsonProperty("title")
        private String title;
        @JsonProperty("body")
        private String body;
    }

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class ExistingKeywordInput {
        @JsonProperty("keyword_id")
        private Long keywordId;
        @JsonProperty("word")
        private String word;
        @JsonProperty("normalized_word")
        private String normalizedWord;
        @JsonProperty("embedding")
        private float[] embedding;
    }

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class NewsAnalysisResult {
        @JsonProperty("news_id")
        private Long newsId;
        @JsonProperty("embedding")
        private float[] embedding;
        @JsonProperty("summary")
        private String summary;
        @JsonProperty("keywords")
        private List<KeywordAiResult> keywords;
    }

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class KeywordAiResult {
        @JsonProperty("keyword_id")
        private Long keywordId;
        @JsonProperty("word")
        private String word;
        @JsonProperty("normalized_word")
        private String normalizedWord;
        @JsonProperty("embedding")
        private float[] embedding;
        @JsonProperty("weight")
        private double weight;
        @JsonProperty("is_new")
        private boolean isNew;
        @JsonProperty("aliases")
        private List<String> aliases;
        @JsonProperty("personas")
        private String personas;
        @JsonProperty("macro")
        private String macro;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KeywordRelationResult {
        @JsonProperty("subject_keyword_id")
        private Long subjectKeywordId;
        @JsonProperty("related_keyword_id")
        private Long relatedKeywordId;
        @JsonProperty("subject_normalized_word")
        private String subjectNormalizedWord;
        @JsonProperty("related_normalized_word")
        private String relatedNormalizedWord;
        @JsonProperty("relation_score")
        private double relationScore;
    }
}