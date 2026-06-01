package nodingo.core.graph.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nodingo.core.graph.dto.result.GraphEdgeResult;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphEdgeResponse {
    private Long source;
    private Long target;
    private double weight;

    public static GraphEdgeResponse from(GraphEdgeResult result) {
        return GraphEdgeResponse.builder()
                .source(result.getSource())
                .target(result.getTarget())
                .weight(result.getWeight())
                .build();
    }
}