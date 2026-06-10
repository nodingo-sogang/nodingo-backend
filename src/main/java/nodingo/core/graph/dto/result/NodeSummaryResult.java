package nodingo.core.graph.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nodingo.core.keyword.domain.Keyword;
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

    public static NodeSummaryResult from(RecommendKeyword rk, List<NewsItemBrief> news, boolean hasNext) {
        return NodeSummaryResult.builder()
                .keywordId(rk.getKeyword().getId())
                .word(rk.getKeyword().getWord())
                .persona(rk.getKeyword().getPersona().name())
                .summary(rk.getSummary())
                .news(news)
                .hasNext(hasNext)
                .build();
    }

    public static NodeSummaryResult from(RecommendKeyword rk) {
        return NodeSummaryResult.builder()
                .keywordId(rk.getKeyword().getId())
                .word(rk.getKeyword().getWord())
                .persona(rk.getKeyword().getPersona().name())
                .summary(rk.getSummary())
                .news(null)
                .hasNext(false)
                .build();
    }

    public static NodeSummaryResult from(Keyword keyword) {
        return NodeSummaryResult.builder()
                .keywordId(keyword.getId())
                .word(keyword.getWord())
                .persona(keyword.getPersona().name())
                .summary("탐색 그래프에서 직접 스크랩한 키워드입니다. 상세 그래프 뷰에서 이 키워드와 얽힌 경제 뉴스 관계망을 확인해보세요!")
                .news(null)
                .hasNext(false)
                .build();
    }
}