package nodingo.core.graph.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nodingo.core.ai.dto.graphPreview.GraphPreview;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphEdgeResult {
    private Long source;
    private Long target;
    private double weight;

    public static GraphEdgeResult from(GraphPreview.GraphEdge aiEdge) {
        return GraphEdgeResult.builder()
                .source(aiEdge.getSource())
                .target(aiEdge.getTarget())
                .weight(aiEdge.getWeight())
                .build();

}