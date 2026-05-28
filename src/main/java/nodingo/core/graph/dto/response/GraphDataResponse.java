package nodingo.core.graph.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nodingo.core.graph.dto.result.GraphDataResult;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphDataResponse {
    private List<GraphNodeResponse> nodes;
    private List<GraphEdgeResponse> edges;

    public static GraphDataResponse from(GraphDataResult result) {
        return GraphDataResponse.builder()
                .nodes(result.getNodes().stream()
                        .map(GraphNodeResponse::from)
                        .collect(Collectors.toList()))
                .edges(result.getEdges().stream()
                        .map(GraphEdgeResponse::from)
                        .collect(Collectors.toList()))
                .build();
    }
}
