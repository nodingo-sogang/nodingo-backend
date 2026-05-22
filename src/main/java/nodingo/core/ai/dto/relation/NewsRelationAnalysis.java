package nodingo.core.ai.dto.relation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class NewsRelationAnalysis {

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class Request {
        @JsonProperty("news")
        private List<NewsEmbeddingInput> newsEmbeddings;

        @JsonProperty("min_score")
        private Double similarityThreshold;

        @JsonProperty("top_k")
        private Integer topK;
    }

    @Getter
    @NoArgsConstructor @AllArgsConstructor
    public static class Response {
        @JsonProperty("news_relations")
        private List<RelationResult> relations;
    }

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class NewsEmbeddingInput {
        @JsonProperty("news_id")
        private Long newsId;
        @JsonProperty("embedding")
        private float[] embedding;
    }

    @Getter
    @NoArgsConstructor @AllArgsConstructor
    public static class RelationResult {
        @JsonProperty("subject_news_id")
        private Long subjectNewsId;
        @JsonProperty("related_news_id")
        private Long relatedNewsId;
        @JsonProperty("relation_score")
        private Double relationScore;
    }
}