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
public class GraphNodeResult {
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

    public static GraphNodeResult from(GraphPreview.GraphNode aiNode, boolean explored, boolean scrapped, int newsCount) {
        return GraphNodeResult.builder()
                .id(aiNode.getId())
                .label(aiNode.getLabel())
                .score(aiNode.getScore())
                .summary(aiNode.getSummary())
                .persona(aiNode.getPersona())
                .unlockLevel(aiNode.getUnlockLevel() != null ? aiNode.getUnlockLevel() : 1)
                .visibility(aiNode.getVisibility() != null ? aiNode.getVisibility() : "VISIBLE")
                .explored(explored)
                .scrapped(scrapped)
                .newsCount(newsCount)
                .build();
    }
}