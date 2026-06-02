package nodingo.core.graph.service.query;

import nodingo.core.ai.client.AiClient;
import nodingo.core.ai.dto.graphPreview.GraphPreview;
import nodingo.core.graph.dto.result.GraphDataResult;
import nodingo.core.graph.dto.result.NodeSummaryResult;
import nodingo.core.graph.dto.result.TabListResult;
import nodingo.core.graph.service.command.NeighborSummaryService;
import nodingo.core.keyword.domain.Keyword;
import nodingo.core.keyword.domain.KeywordRelation;
import nodingo.core.keyword.domain.RecommendKeyword;
import nodingo.core.keyword.repository.KeywordRelationRepository;
import nodingo.core.keyword.repository.NewsKeywordRepository;
import nodingo.core.keyword.repository.RecommendKeywordRepository;
import nodingo.core.keyword.repository.UserKeywordExploreRepository;
import nodingo.core.news.domain.News;
import nodingo.core.user.domain.UserPersona;
import nodingo.core.user.repository.UserInterestRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GraphQueryServiceTest {

    @InjectMocks
    private GraphQueryService graphQueryService;

    @Mock
    private AiClient aiClient;

    @Mock
    private RecommendKeywordRepository recommendKeywordRepository;

    @Mock
    private KeywordRelationRepository keywordRelationRepository;

    @Mock
    private UserKeywordExploreRepository exploreRepository;

    @Mock
    private UserInterestRepository interestRepository;

    @Mock
    private NewsKeywordRepository newsKeywordRepository;

    @Mock
    private NeighborSummaryService neighborSummaryService;

    @Test
    @DisplayName("API 1: 오늘의 추천 탭 목록 조회 테스트")
    void getTodayTabs_Success() {
        // given
        Long userId = 1L;
        RecommendKeyword rk1 = createRecommendKeyword(101L, "트럼프", 0.9);
        RecommendKeyword rk2 = createRecommendKeyword(102L, "관세", 0.8);

        when(recommendKeywordRepository.findTabsByUserAndDate(eq(userId), any(LocalDate.class)))
                .thenReturn(List.of(rk1, rk2));

        // when
        TabListResult result = graphQueryService.getTodayTabs(userId);

        // then
        assertThat(result.getTabs()).hasSize(2);
        assertThat(result.getTabs().get(0).getWord()).isEqualTo("트럼프");
        verify(recommendKeywordRepository, times(1)).findTabsByUserAndDate(eq(userId), any(LocalDate.class));
    }

    @Test
    @DisplayName("API 2-1: 그래프 프리뷰 전체 조회 테스트 (keywordId가 null일 때)")
    void getGraphPreview_GlobalView_Success() {
        // given
        Long userId = 1L;
        RecommendKeyword rk1 = createRecommendKeyword(101L, "트럼프", 0.9);

        when(recommendKeywordRepository.findTabsByUserAndDate(eq(userId), any(LocalDate.class)))
                .thenReturn(List.of(rk1));

        Page<KeywordRelation> mockRelationPage = new PageImpl<>(List.of());
        when(keywordRelationRepository.findTopRelations(anyLong(), any(Pageable.class)))
                .thenReturn(mockRelationPage);

        GraphPreview.Response mockAiResponse = GraphPreview.Response.builder()
                .nodes(List.of(GraphPreview.GraphNode.builder().id(101L).label("트럼프").summary("요약본").build()))
                .edges(List.of())
                .build();
        when(aiClient.getGraphPreview(any())).thenReturn(mockAiResponse);

        when(exploreRepository.findExploredKeywordIdsByUserId(userId)).thenReturn(Set.of());
        when(interestRepository.findScrappedKeywordIdsByUserId(userId)).thenReturn(Set.of());
        when(newsKeywordRepository.countNewsByKeywordIds(anyList())).thenReturn(Map.of());

        // when
        GraphDataResult result = graphQueryService.getGraphPreview(userId, null);

        // then
        assertThat(result.getNodes()).hasSize(1);
        verify(keywordRelationRepository, times(1)).findTopRelations(eq(101L), any(Pageable.class));
    }

    @Test
    @DisplayName("API 2-2: 그래프 프리뷰 특정 탭 조회 테스트 (keywordId가 존재할 때)")
    void getGraphPreview_FocusView_Success() {
        // given
        Long userId = 1L;
        Long keywordId = 101L;
        RecommendKeyword rk = createRecommendKeyword(keywordId, "트럼프", 0.9);

        when(recommendKeywordRepository.findTabsByUserAndDate(eq(userId), any(LocalDate.class)))
                .thenReturn(List.of(rk));

        Page<KeywordRelation> mockRelationPage = new PageImpl<>(List.of());
        when(keywordRelationRepository.findTopRelations(eq(keywordId), any(Pageable.class)))
                .thenReturn(mockRelationPage);

        GraphPreview.Response mockAiResponse = GraphPreview.Response.builder()
                .nodes(List.of(GraphPreview.GraphNode.builder().id(keywordId).label("트럼프").summary("요약본").build()))
                .edges(List.of())
                .build();
        when(aiClient.getGraphPreview(any())).thenReturn(mockAiResponse);

        when(exploreRepository.findExploredKeywordIdsByUserId(userId)).thenReturn(Set.of());
        when(interestRepository.findScrappedKeywordIdsByUserId(userId)).thenReturn(Set.of());
        when(newsKeywordRepository.countNewsByKeywordIds(anyList())).thenReturn(Map.of());

        // when
        GraphDataResult result = graphQueryService.getGraphPreview(userId, keywordId);

        // then
        assertThat(result.getNodes()).isNotEmpty();
        assertThat(result.getNodes().get(0).getLabel()).isEqualTo("트럼프");
        verify(keywordRelationRepository, times(1)).findTopRelations(eq(keywordId), any(Pageable.class));
    }

    @Test
    @DisplayName("API 3: 특정 노드 상세 요약 및 연관 뉴스 Slice 페이징 조회 테스트")
    void getNodeSummary_Success() {
        // given
        Long userId = 1L;
        Long keywordId = 101L;
        Pageable pageable = PageRequest.of(0, 10);
        RecommendKeyword rk = createRecommendKeyword(keywordId, "트럼프", 0.9);

        when(recommendKeywordRepository.findByUserIdAndKeywordIdAndTargetDate(eq(userId), eq(keywordId), any(LocalDate.class)))
                .thenReturn(Optional.of(rk));

        News mockNews = mock(News.class);
        when(mockNews.getId()).thenReturn(1L);
        when(mockNews.getTitle()).thenReturn("트럼프 관세 폭탄 기사");
        when(mockNews.getBody()).thenReturn("뉴스 본문 내용입니다. 아주 긴 기사 본문 스니펫 테스트...");

        Slice<News> mockSlice = new SliceImpl<>(List.of(mockNews), pageable, true);
        when(newsKeywordRepository.findNewsSliceByKeywordId(eq(keywordId), eq(pageable)))
                .thenReturn(mockSlice);

        // when
        NodeSummaryResult result = graphQueryService.getNodeSummary(userId, keywordId, pageable);

        // then
        assertThat(result.getSummary()).isEqualTo("테스트 요약본입니다.");
        assertThat(result.getWord()).isEqualTo("트럼프");
        assertThat(result.getNews()).hasSize(1);
        assertThat(result.getNews().get(0).getTitle()).isEqualTo("트럼프 관세 폭탄 기사");
        assertThat(result.isHasNext()).isTrue();

        verify(recommendKeywordRepository, times(1)).findByUserIdAndKeywordIdAndTargetDate(eq(userId), eq(keywordId), any(LocalDate.class));
        verify(newsKeywordRepository, times(1)).findNewsSliceByKeywordId(eq(keywordId), eq(pageable));
    }

    private RecommendKeyword createRecommendKeyword(Long id, String word, double score) {
        Keyword k = Keyword.create(word, LocalDate.now());
        ReflectionTestUtils.setField(k, "id", id);
        ReflectionTestUtils.setField(k, "persona", UserPersona.POLITICS);

        RecommendKeyword rk = RecommendKeyword.create(null, k, LocalDate.now(), score);

        ReflectionTestUtils.setField(rk, "id", 1L);
        rk.updateSummary("테스트 요약본입니다.");

        return rk;
    }
}