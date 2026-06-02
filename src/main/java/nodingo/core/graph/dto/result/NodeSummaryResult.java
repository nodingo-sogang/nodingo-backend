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
    private boolean hasNext;

    // 퀴즈/뉴스 함께 넘길 때 사용하는 생성 팩토리
    public static NodeSummaryResult from(RecommendKeyword rk, List<NewsItemBrief> news, boolean hasNext) {
        return NodeSummaryResult.builder()
                .keywordId(rk.getKeyword().getId())
                .word(rk.getKeyword().getWord())
                .persona(rk.getKeyword().getPersona() != null ? rk.getKeyword().getPersona().name() : null)
                .summary(rk.getSummary())
                .news(news)
                .hasNext(hasNext)
                .build();
    }

    // 뉴스 없이 요약만 넘길 때 (디폴트 false 처리)
    public static NodeSummaryResult from(RecommendKeyword rk) {
        return NodeSummaryResult.builder()
                .keywordId(rk.getKeyword().getId())
                .word(rk.getKeyword().getWord())
                .persona(rk.getKeyword().getPersona() != null ? rk.getKeyword().getPersona().name() : null)
                .summary(rk.getSummary())
                .news(null)
                .hasNext(false)
                .build();
    }
}