package nodingo.core.graph.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nodingo.core.keyword.domain.RecommendKeyword;

import nodingo.core.graph.dto.NewsItemBrief;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NodeSummaryResult {
    private Long keywordId;
    private String word;
    private String persona;
    private String summary;
    private List<NewsItemBrief> news;

    public static NodeSummaryResult from(RecommendKeyword rk, List<NewsItemBrief> news) {
        return NodeSummaryResult.builder()
                .keywordId(rk.getKeyword().getId())
                .word(rk.getKeyword().getWord())
                .persona(rk.getKeyword().getPersona() != null ? rk.getKeyword().getPersona().name() : null)
                .summary(rk.getSummary())
                .news(news)
                .build();
    }

    public static NodeSummaryResult from(RecommendKeyword rk) {
        return NodeSummaryResult.builder()
                .keywordId(rk.getKeyword().getId())
                .word(rk.getKeyword().getWord())
                .persona(rk.getKeyword().getPersona() != null ? rk.getKeyword().getPersona().name() : null)
                .summary(rk.getSummary())
                .news(null)
                .build();
    }
}