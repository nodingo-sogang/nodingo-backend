package nodingo.core.keyword.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import nodingo.core.global.auth.CustomOAuth2User;
import nodingo.core.global.dto.response.ApiResponse;
import nodingo.core.graph.dto.response.NodeSummaryResponse;
import nodingo.core.graph.dto.result.NodeSummaryResult;
import nodingo.core.keyword.dto.response.ScrapKeywordNodeResponse;
import nodingo.core.keyword.dto.result.ScrapKeywordNodeResult;
import nodingo.core.keyword.service.command.RecommendKeywordScrapService;
import nodingo.core.keyword.service.query.RecommendKeywordScrapQueryService;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Keyword Scrap", description = "그래프 노드(키워드 요약) 스크랩 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class RecommendKeywordScrapController {

    private final RecommendKeywordScrapService keywordScrapService;
    private final RecommendKeywordScrapQueryService keywordScrapQueryService;

    @Operation(summary = "키워드 요약 스크랩 추가",
            description = "그래프 노드의 상세 요약 정보를 내 보관함에 저장합니다. 이미 스크랩한 경우 409 에러가 발생합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "키워드 스크랩 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "키워드/사용자 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 스크랩한 키워드")
    })
    @PostMapping("/keywords/{keywordId}/scrap")
    public ResponseEntity<ApiResponse<Void>> addScrap(
            @AuthenticationPrincipal CustomOAuth2User customUser,
            @PathVariable Long keywordId) {
        keywordScrapService.addScrap(customUser.getUser().getId(), keywordId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, 201, "키워드 스크랩 성공", null));
    }

    @Operation(summary = "키워드 요약 스크랩 취소",
            description = "스크랩한 키워드 요약을 보관함에서 삭제합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "키워드 스크랩 취소 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "스크랩하지 않은 키워드")
    })
    @DeleteMapping("/keywords/{keywordId}/scrap")
    public ResponseEntity<ApiResponse<Void>> removeScrap(
            @AuthenticationPrincipal CustomOAuth2User customUser,
            @PathVariable Long keywordId) {
        keywordScrapService.removeScrap(customUser.getUser().getId(), keywordId);
        return ResponseEntity.ok(new ApiResponse<>(true, 200, "키워드 스크랩 취소 성공", null));
    }

    @Operation(summary = "스크랩한 키워드 노드 목록 조회 (그래프용)",
            description = "스크랩한 키워드의 ID와 이름 목록을 20개씩 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/users/scraps/keywords/nodes")
    public ResponseEntity<ApiResponse<Slice<ScrapKeywordNodeResponse>>> getScrapKeywordNodes(
            @AuthenticationPrincipal CustomOAuth2User customUser,
            @RequestParam(defaultValue = "0") int page) {
        Slice<ScrapKeywordNodeResult> result = keywordScrapQueryService
                .getScrapKeywordNodes(customUser.getUser().getId(), page);
        Slice<ScrapKeywordNodeResponse> response = result.map(ScrapKeywordNodeResponse::from);
        return ResponseEntity.ok(new ApiResponse<>(true, 200, "스크랩한 키워드 노드 목록을 조회했습니다.", response));
    }

    @Operation(summary = "스크랩한 키워드 요약 목록 조회 (목록용)",
            description = "스크랩한 키워드의 요약 카드 목록을 4개씩 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/users/scraps/keywords/summaries")
    public ResponseEntity<ApiResponse<Slice<NodeSummaryResponse>>> getScrapKeywordSummaries(
            @AuthenticationPrincipal CustomOAuth2User customUser,
            @RequestParam(defaultValue = "0") int page) {
        Slice<NodeSummaryResult> result = keywordScrapQueryService
                .getScrapKeywordSummaries(customUser.getUser().getId(), page);
        Slice<NodeSummaryResponse> response = result.map(NodeSummaryResponse::from);
        return ResponseEntity.ok(new ApiResponse<>(true, 200, "스크랩한 키워드 요약 목록을 조회했습니다.", response));
    }
}
