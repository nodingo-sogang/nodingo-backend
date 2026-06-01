package nodingo.core.graph.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nodingo.core.graph.dto.result.NodeSummaryResult;

import nodingo.core.graph.dto.NewsItemBrief;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NodeSummaryResponse {
    private Long keywordId;
    private String word;
    private String persona;
    private String summary;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<NewsItemBrief> news;

    public static NodeSummaryResponse from(NodeSummaryResult result) {
        return NodeSummaryResponse.builder()
                .keywordId(result.getKeywordId())
                .word(result.getWord())
                .persona(result.getPersona())
                .summary(result.getSummary())
                .news(result.getNews())
                .build();
    }
}