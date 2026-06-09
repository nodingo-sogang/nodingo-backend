package nodingo.core.graph.service.query;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nodingo.core.ai.client.AiClient;
import nodingo.core.global.exception.ai.AiRateLimitException;
import nodingo.core.global.metrics.MonitoringMetrics;
import nodingo.core.ai.dto.graphPreview.GraphPreview;
import nodingo.core.global.util.DateUtil;
import nodingo.core.graph.dto.result.*;
import nodingo.core.graph.service.command.NeighborSummaryService;
import nodingo.core.keyword.domain.Keyword;
import nodingo.core.keyword.domain.KeywordRelation;
import nodingo.core.keyword.domain.RecommendKeyword;
import nodingo.core.keyword.repository.KeywordRelationRepository;
import nodingo.core.keyword.repository.NewsKeywordRepository;
import nodingo.core.keyword.repository.RecommendKeywordRepository;
import nodingo.core.keyword.repository.UserKeywordExploreRepository;
import nodingo.core.news.domain.News;
import nodingo.core.graph.dto.NewsItemBrief;
import nodingo.core.user.repository.UserInterestRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GraphQueryService {
    private static final int KEYWORD_LIMIT = 20;
    private static final int RELATED_KEYWORD_LIMIT = 30;

    private final AiClient aiClient;
    private final RecommendKeywordRepository recommendKeywordRepository;
    private final KeywordRelationRepository keywordRelationRepository;
    private final UserKeywordExploreRepository exploreRepository;
    private final UserInterestRepository interestRepository;
    private final NewsKeywordRepository newsKeywordRepository;
    private final NeighborSummaryService neighborSummaryService;
    private final MonitoringMetrics metrics;

    public TabListResult getTodayTabs(Long userId) {
        LocalDate targetDate = DateUtil.getTargetDate();
        List<RecommendKeyword> recommendKeywords = recommendKeywordRepository.findTabsByUserAndDate(userId, targetDate);
        log.info(">>>> [Graph] getTodayTabs. userId={}, targetDate={}, tabs={}", userId, targetDate, recommendKeywords.size());
        List<TabResult> tabs = getTabResults(recommendKeywords);
        return TabListResult.of(tabs);
    }

    @Cacheable(value = "batch:graph", key = "#userId + ':' + (#centralKeywordId == null ? 'ALL' : #centralKeywordId)")
    public GraphDataResult getGraphPreview(Long userId, Long centralKeywordId) {

        Map<Long, RecommendKeyword> recommendMap = getRecommendKeywordMap(userId);
        List<KeywordRelation> targetRelations;

        if (centralKeywordId == null) {
            List<Long> topTabIds = getTopTabIds(recommendMap);
            targetRelations = topTabIds.stream()
                    .flatMap(id -> keywordRelationRepository
                            .findTopRelations(id, PageRequest.of(0, RELATED_KEYWORD_LIMIT))
                            .getContent().stream())
                    .distinct()
                    .toList();
        } else {
            targetRelations = keywordRelationRepository
                    .findTopRelations(centralKeywordId, PageRequest.of(0, RELATED_KEYWORD_LIMIT))
                    .getContent();
        }

        log.info(">>>> [Graph] centralKeywordId={}, targetRelations size={}", centralKeywordId, targetRelations.size());

        GraphPreview.Request aiRequest = createAiRequest(centralKeywordId, targetRelations, recommendMap);

        GraphPreview.Response aiResponse;
        try {
            metrics.recordAiCall("graph.preview");
            aiResponse = aiClient.getGraphPreview(aiRequest);
            log.info(">>>> [Graph] AI response nodes={}, edges={}", aiResponse.getNodes().size(), aiResponse.getEdges().size());
        } catch (AiRateLimitException e) {
            metrics.recordAiFailure("graph.preview", "RateLimitError");
            log.error(">>>> [Graph] OpenAI rate limit exceeded (429). userId={}", userId, e);
            throw e;
        } catch (Exception e) {
            metrics.recordAiFailure("graph.preview", e.getClass().getSimpleName());
            log.error(">>>> [Graph] AI call failed. userId={}", userId, e);
            throw e;
        }

        GraphPreview.Response filteredResponse = filterUnknownNodes(aiResponse);

        List<Long> emptySummaryNodeIds = filteredResponse.getNodes().stream()
                .filter(node -> node.getSummary() == null || node.getSummary().isBlank())
                .map(GraphPreview.GraphNode::getId)
                .toList();


        Map<Long, String> generatedSummaries = new HashMap<>();
        if (!emptySummaryNodeIds.isEmpty()) {
            log.info(">>>> [Graph] Generating summaries for {} empty nodes.", emptySummaryNodeIds.size());
            generatedSummaries = neighborSummaryService.generateSummarySync(emptySummaryNodeIds);
            neighborSummaryService.generateQuizAsync(emptySummaryNodeIds, generatedSummaries);
            filteredResponse = injectSummaries(filteredResponse, generatedSummaries);
        }

        List<Long> nodeIds = filteredResponse.getNodes().stream()
                .map(GraphPreview.GraphNode::getId)
                .toList();

        Set<Long> exploredIds = exploreRepository.findExploredKeywordIdsByUserId(userId);
        Set<Long> scrappedIds = interestRepository.findScrappedKeywordIdsByUserId(userId);
        Map<Long, Integer> newsCountMap = newsKeywordRepository.countNewsByKeywordIds(nodeIds);

        return buildGraphDataResult(filteredResponse, exploredIds, scrappedIds, newsCountMap);
    }

    private GraphPreview.Response injectSummaries(GraphPreview.Response response, Map<Long, String> summaryMap) {
        List<GraphPreview.GraphNode> updatedNodes = response.getNodes().stream()
                .map(node -> {
                    String summary = summaryMap.get(node.getId());
                    if (summary != null) {
                        return GraphPreview.GraphNode.builder()
                                .id(node.getId())
                                .label(node.getLabel())
                                .score(node.getScore())
                                .summary(summary)
                                .persona(node.getPersona())
                                .unlockLevel(node.getUnlockLevel())
                                .visibility(node.getVisibility())
                                .build();
                    }
                    return node;
                })
                .toList();

        return GraphPreview.Response.builder()
                .nodes(updatedNodes)
                .edges(response.getEdges())
                .build();
    }

    private static List<Long> getTopTabIds(Map<Long, RecommendKeyword> recommendMap) {
        return recommendMap.values().stream()
                .sorted(Comparator.comparingDouble(RecommendKeyword::getScore).reversed())
                .limit(KEYWORD_LIMIT)
                .map(rk -> rk.getKeyword().getId())
                .toList();
    }

    public NodeSummaryResult getNodeSummary(Long userId, Long nodeId, Pageable pageable) {
        log.info(">>>> [Graph] getNodeSummary. userId={}, nodeId={}", userId, nodeId);
        LocalDate targetDate = DateUtil.getTargetDate();

        RecommendKeyword recommendKeyword = recommendKeywordRepository
                .findByUserIdAndKeywordIdAndTargetDate(userId, nodeId, targetDate)
                .orElseThrow(() -> new IllegalArgumentException("해당 키워드에 대한 요약 정보를 찾을 수 없습니다."));

        Slice<News> relatedNewsSlice = newsKeywordRepository.findNewsSliceByKeywordId(nodeId, pageable);

        List<NewsItemBrief> newsBriefs = relatedNewsSlice.getContent().stream()
                .map(news -> {
                    String safeSnippet = news.getBody();
                    if (safeSnippet != null && safeSnippet.length() > 100) {
                        safeSnippet = safeSnippet.substring(0, 100) + "...";
                    }
                    return NewsItemBrief.builder()
                            .id(news.getId())
                            .title(news.getTitle())
                            .url(news.getUrl())
                            .outlet("뉴스 원문")
                            .date(news.getDateTimePub() != null ? news.getDateTimePub().toLocalDate() : null)
                            .snippet(safeSnippet)
                            .build();
                })
                .toList();

        return NodeSummaryResult.from(recommendKeyword, newsBriefs, relatedNewsSlice.hasNext());
    }

    private static List<TabResult> getTabResults(List<RecommendKeyword> recommendKeywords) {
        return recommendKeywords.stream()
                .sorted(Comparator.comparingDouble(RecommendKeyword::getScore).reversed())
                .limit(KEYWORD_LIMIT)
                .map(TabResult::of)
                .toList();
    }

    private GraphPreview.Request createAiRequest(Long centralId, List<KeywordRelation> relations, Map<Long, RecommendKeyword> recommendMap) {
        Set<Long> nodeIds = extractAllNodeIds(centralId, relations);

        Map<Long, Keyword> relationKeywordMap = new HashMap<>();
        relations.forEach(rel -> {
            relationKeywordMap.put(rel.getSubjectKeyword().getId(), rel.getSubjectKeyword());
            relationKeywordMap.put(rel.getRelatedKeyword().getId(), rel.getRelatedKeyword());
        });

        return GraphPreview.Request.builder()
                .recommendKeywords(mapToKeywordInputs(nodeIds, recommendMap, relationKeywordMap))
                .keywordRelations(mapToRelationInputs(relations))
                .build();
    }

    private Set<Long> extractAllNodeIds(Long centralId, List<KeywordRelation> relations) {
        Set<Long> ids = new HashSet<>();
        if (centralId != null) ids.add(centralId);
        relations.forEach(rel -> {
            ids.add(rel.getSubjectKeyword().getId());
            ids.add(rel.getRelatedKeyword().getId());
        });
        return ids;
    }

    private List<GraphPreview.GraphRecommendKeywordInput> mapToKeywordInputs(
            Set<Long> nodeIds,
            Map<Long, RecommendKeyword> recommendMap,
            Map<Long, Keyword> relationKeywordMap) {

        return nodeIds.stream()
                .map(id -> {
                    RecommendKeyword rk = recommendMap.get(id);
                    Keyword fallback = relationKeywordMap.get(id);
                    return GraphPreview.GraphRecommendKeywordInput.builder()
                            .keywordId(id)
                            .word(rk != null ? rk.getKeyword().getWord()
                                    : fallback != null ? fallback.getWord() : "Unknown")
                            .score(rk != null ? rk.getScore() : 0.45)
                            .summary(rk != null ? rk.getSummary() : "")
                            .persona(rk != null && rk.getKeyword().getPersona() != null
                                    ? rk.getKeyword().getPersona().name()
                                    : fallback != null && fallback.getPersona() != null
                                    ? fallback.getPersona().name() : null)
                            .build();
                }).toList();
    }

    private List<GraphPreview.GraphKeywordRelationInput> mapToRelationInputs(List<KeywordRelation> relations) {
        return relations.stream()
                .map(rel -> GraphPreview.GraphKeywordRelationInput.builder()
                        .sourceKeywordId(rel.getSubjectKeyword().getId())
                        .targetKeywordId(rel.getRelatedKeyword().getId())
                        .relationScore(rel.getRelationScore())
                        .build())
                .toList();
    }

    private GraphDataResult buildGraphDataResult(
            GraphPreview.Response response,
            Set<Long> exploredIds,
            Set<Long> scrappedIds,
            Map<Long, Integer> newsCountMap) {

        List<GraphNodeResult> nodes = response.getNodes().stream()
                .map(node -> GraphNodeResult.from(
                        node,
                        exploredIds.contains(node.getId()),
                        scrappedIds.contains(node.getId()),
                        newsCountMap.getOrDefault(node.getId(), 0)
                ))
                .toList();

        List<GraphEdgeResult> edges = response.getEdges().stream()
                .map(GraphEdgeResult::from)
                .toList();

        return new GraphDataResult(nodes, edges);
    }

    private GraphPreview.Response filterUnknownNodes(GraphPreview.Response response) {
        List<GraphPreview.GraphNode> filteredNodes = response.getNodes().stream()
                .filter(node -> !"Unknown".equals(node.getLabel()))
                .toList();

        Set<Long> validNodeIds = filteredNodes.stream()
                .map(GraphPreview.GraphNode::getId)
                .collect(Collectors.toSet());

        List<GraphPreview.GraphEdge> filteredEdges = response.getEdges().stream()
                .filter(edge -> validNodeIds.contains(edge.getSource())
                        && validNodeIds.contains(edge.getTarget()))
                .toList();

        return GraphPreview.Response.builder()
                .nodes(filteredNodes)
                .edges(filteredEdges)
                .build();
    }

    private Map<Long, RecommendKeyword> getRecommendKeywordMap(Long userId) {
        LocalDate targetDate = DateUtil.getTargetDate();

        return recommendKeywordRepository.findTabsByUserAndDate(userId, targetDate).stream()
                .collect(Collectors.toMap(
                        rk -> rk.getKeyword().getId(),
                        rk -> rk,
                        (existing, replacement) -> existing
                ));
    }
}