package nodingo.core.ai.dto.graphPreview;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

public class GraphPreview {

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class Request {
        @JsonProperty("recommend_keywords")
        private List<GraphRecommendKeywordInput> recommendKeywords;

        @JsonProperty("keyword_relations")
        private List<GraphKeywordRelationInput> keywordRelations;
    }

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class GraphRecommendKeywordInput {
        @JsonProperty("keyword_id")
        private Long keywordId;

        @JsonProperty("word")
        private String word;

        @JsonProperty("score")
        private double score;

        @JsonProperty("summary")
        private String summary;

        @JsonProperty("persona")
        private String persona;
    }

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class GraphKeywordRelationInput {
        @JsonProperty("source_keyword_id")
        private Long sourceKeywordId;

        @JsonProperty("target_keyword_id")
        private Long targetKeywordId;

        @JsonProperty("relation_score")
        private double relationScore;
    }

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class Response {
        @JsonProperty("nodes")
        private List<GraphNode> nodes;

        @JsonProperty("edges")
        private List<GraphEdge> edges;
    }

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class GraphNode {
        @JsonProperty("id")
        private Long id;

        @JsonProperty("label")
        private String label;

        @JsonProperty("score")
        private double score;

        @JsonProperty("summary")
        private String summary;

        @JsonProperty("persona")
        private String persona;
    }

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class GraphEdge {
        @JsonProperty("source")
        private Long source;

        @JsonProperty("target")
        private Long target;

        @JsonProperty("weight")
        private double weight;
    }
}