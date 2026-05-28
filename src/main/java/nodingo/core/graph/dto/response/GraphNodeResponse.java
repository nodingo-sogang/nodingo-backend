package nodingo.core.graph.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nodingo.core.graph.dto.result.GraphNodeResult;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphNodeResponse {
    private Long id;
    private String label;
    private double score;
    private String summary;
    private String persona;

    private Integer unlockLevel;
    private String visibility;
    private Boolean explored;
    private Boolean scrapped;
    private Integer newsCount;

    public static GraphNodeResponse from(GraphNodeResult result) {
        return GraphNodeResponse.builder()
                .id(result.getId())
                .label(result.getLabel())
                .score(result.getScore())
                .summary(result.getSummary())
                .persona(result.getPersona())
                .unlockLevel(result.getUnlockLevel())
                .visibility(result.getVisibility())
                .explored(result.getExplored())
                .scrapped(result.getScrapped())
                .newsCount(result.getNewsCount())
                .build();
    }
}