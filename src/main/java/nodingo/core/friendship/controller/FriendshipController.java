package nodingo.core.friendship.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nodingo.core.friendship.dto.command.FriendActionCommand;
import nodingo.core.friendship.dto.request.FriendActionRequest;
import nodingo.core.friendship.dto.response.FriendListResponse;
import nodingo.core.friendship.dto.result.FriendListResult;
import nodingo.core.friendship.service.command.FriendshipService;
import nodingo.core.friendship.service.query.FriendshipQueryService;
import nodingo.core.global.annotation.RequireOnboardingCompleted;
import nodingo.core.global.auth.CustomOAuth2User;
import nodingo.core.global.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Friendship", description = "친구 관계 및 인앱 친구 요청 상태 머신 관련 API")
@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendshipController {

    private final FriendshipService friendshipService;
    private final FriendshipQueryService friendshipQueryService;

    @Operation(
            summary = "친구 요청 보내기",
            description = "상대방의 유저 ID를 받아 친구 신청(PENDING)을 보냅니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "친구 요청을 성공적으로 보냈습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "본인에게 요청을 보낸 경우 에러"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "상대방 유저를 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 친구 관계이거나 이미 요청이 진행 중인 경우 에러")
    })
    @RequireOnboardingCompleted
    @PostMapping("/request")
    public ResponseEntity<ApiResponse<Void>> sendRequest(
            @AuthenticationPrincipal CustomOAuth2User customOAuth2User,
            @Valid @RequestBody FriendActionRequest request) {

        FriendActionCommand command = FriendActionCommand.of(customOAuth2User.getUser().getId(), request.getTargetUserId());
        friendshipService.sendFriendRequest(command);
        return ResponseEntity.ok(new ApiResponse<>(true, 200, "친구 요청을 성공적으로 보냈습니다.", null));
    }

    @Operation(
            summary = "나에게 온 요청 목록 조회",
            description = "나한테 온 수락 대기(PENDING) 상태인 유저 목록을 조회합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "받은 친구 요청 목록을 성공적으로 조회했습니다.")
    })
    @RequireOnboardingCompleted
    @GetMapping("/received")
    public ResponseEntity<ApiResponse<FriendListResponse>> getReceivedRequests(
            @AuthenticationPrincipal CustomOAuth2User customOAuth2User) {

        Long userId = customOAuth2User.getUser().getId();
        FriendListResult result = friendshipQueryService.getPendingRequests(userId);
        return ResponseEntity.ok(new ApiResponse<>(true, 200, "받은 친구 요청 목록을 성공적으로 조회했습니다.", FriendListResponse.from(result)));
    }

    @Operation(
            summary = "친구 요청 수락하기",
            description = "나에게 요청을 보냈던 상대방의 유저 ID를 입력받아 정식 서로친구(ACCEPTED) 상태로 승인합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "친구 요청을 성공적으로 수락했습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "수락 대기 중인 친구 요청을 찾을 수 없음")
    })
    @RequireOnboardingCompleted
    @PostMapping("/accept")
    public ResponseEntity<ApiResponse<Void>> acceptRequest(
            @AuthenticationPrincipal CustomOAuth2User customOAuth2User,
            @Valid @RequestBody FriendActionRequest request) {

        FriendActionCommand command = FriendActionCommand.of(customOAuth2User.getUser().getId(), request.getTargetUserId());
        friendshipService.acceptFriendRequest(command);
        return ResponseEntity.ok(new ApiResponse<>(true, 200, "친구 요청을 성공적으로 수락했습니다.", null));
    }

    @Operation(
            summary = "내 친구 목록 조회",
            description = "내가 신청했든 받았든 서로 수락이 완료된(ACCEPTED) 내 찐 친구 목록을 통합 조회합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "정식 친구 목록을 성공적으로 조회했습니다.")
    })
    @RequireOnboardingCompleted
    @GetMapping
    public ResponseEntity<ApiResponse<FriendListResponse>> getMyAcceptedFriends(
            @AuthenticationPrincipal CustomOAuth2User customOAuth2User) {

        Long userId = customOAuth2User.getUser().getId();
        FriendListResult result = friendshipQueryService.getMyAcceptedFriends(userId);
        return ResponseEntity.ok(new ApiResponse<>(true, 200, "정식 친구 목록을 성공적으로 조회했습니다.", FriendListResponse.from(result)));
    }
}