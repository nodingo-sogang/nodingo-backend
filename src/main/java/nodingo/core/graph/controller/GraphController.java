package nodingo.core.graph.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import nodingo.core.global.annotation.RequireOnboardingCompleted;
import nodingo.core.global.auth.CustomOAuth2User;
import nodingo.core.global.dto.response.ApiResponse;
import nodingo.core.graph.dto.response.GraphDataResponse;
import nodingo.core.graph.dto.response.NodeSummaryResponse;
import nodingo.core.graph.dto.response.TabListResponse;
import nodingo.core.graph.dto.result.GraphDataResult;
import nodingo.core.graph.dto.result.NodeSummaryResult;
import nodingo.core.graph.dto.result.TabListResult;
import nodingo.core.graph.service.query.GraphQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Graph", description = "이슈 맵 그래프 시각화 관련 API")
@RequireOnboardingCompleted
@RestController
@RequestMapping("/api/graphs")
@RequiredArgsConstructor
public class GraphController {

    private final GraphQueryService graphQueryService;

    @Operation(
            summary = "오늘의 추천 탭 목록 조회",
            description = "메인 상단에 노출될 유저 맞춤형 소분류 키워드 탭 리스트를 조회합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공적으로 추천 탭 목록을 조회했습니다.")
    })
    @GetMapping("/tabs")
    public ResponseEntity<ApiResponse<TabListResponse>> getTodayTabs(
            @AuthenticationPrincipal CustomOAuth2User customOAuth2User) {
        TabListResult result = graphQueryService.getTodayTabs(customOAuth2User.getUser().getId());
        return ResponseEntity.ok(new ApiResponse<>(true, 200, "성공적으로 추천 탭 목록을 조회했습니다.", TabListResponse.of(result)));
    }

    @Operation(
            summary = "특정 탭 기준 그래프 관계도 조회",
            description = "클릭한 소분류 키워드를 중심으로 한 주변 노드 및 엣지 데이터를 AI 서버를 통해 조회합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공적으로 그래프 시각화 데이터를 조회했습니다.")
    })
    @GetMapping("/nodes")
    public ResponseEntity<ApiResponse<GraphDataResponse>> getGraphNodes(
            @AuthenticationPrincipal CustomOAuth2User customOAuth2User,
            @RequestParam(required = false) Long keywordId) {
        GraphDataResult result = graphQueryService.getGraphPreview(customOAuth2User.getUser().getId(), keywordId);
        return ResponseEntity.ok(new ApiResponse<>(true, 200, "성공적으로 그래프 시각화 데이터를 조회했습니다.", GraphDataResponse.from(result)));
    }

    @Operation(
            summary = "특정 노드(소분류 키워드) 상세 요약 조회",
            description = "그래프 내 노드 클릭 시 우측 패널에 표시될 상세 요약 정보를 조회합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공적으로 키워드 요약 정보를 조회했습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "해당 키워드에 대한 요약 정보를 찾을 수 없습니다.")
    })
    @GetMapping("/nodes/{nodeId}/summaries")
    public ResponseEntity<ApiResponse<NodeSummaryResponse>> getNodeSummary(
            @AuthenticationPrincipal CustomOAuth2User customOAuth2User,
            @PathVariable Long nodeId) {
        NodeSummaryResult result = graphQueryService.getNodeSummary(customOAuth2User.getUser().getId(), nodeId);
        return ResponseEntity.ok(new ApiResponse<>(true, 200, "성공적으로 키워드 요약 정보를 조회했습니다.", NodeSummaryResponse.from(result)));
    }
}