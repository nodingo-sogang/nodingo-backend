package nodingo.core.keyword.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

@Getter
@AllArgsConstructor
public class ScrapGraphResult {
    private List<NodeResult> nodes;
    private List<EdgeResult> edges;

    @Getter
    @AllArgsConstructor
    public static class NodeResult {
        private Long id;
        private String word;
        private String persona;
        private double score;
    }

    @Getter
    @AllArgsConstructor
    public static class EdgeResult {
        private Long source;
        private Long target;
        private double weight;
    }
}