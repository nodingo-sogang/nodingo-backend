package nodingo.core.keyword.dto.result;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nodingo.core.keyword.dto.response.ScrapGraphResult;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ScrapGraphResponse {
    private List<NodeResponse> nodes;
    private List<EdgeResponse> edges;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NodeResponse {
        private Long id;
        private String word;
        private String persona;
        private double score;

        public static NodeResponse from(ScrapGraphResult.NodeResult result) {
            return new NodeResponse(result.getId(), result.getWord(), result.getPersona(), result.getScore());
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EdgeResponse {
        private Long source;
        private Long target;
        private double weight;

        public static EdgeResponse from(ScrapGraphResult.EdgeResult result) {
            return new EdgeResponse(result.getSource(), result.getTarget(), result.getWeight());
        }
    }

    public static ScrapGraphResponse from(ScrapGraphResult result) {
        List<NodeResponse> responseNodes = result.getNodes().stream()
                .map(NodeResponse::from)
                .toList();

        List<EdgeResponse> responseEdges = result.getEdges().stream()
                .map(EdgeResponse::from)
                .toList();

        return new ScrapGraphResponse(responseNodes, responseEdges);
    }
}