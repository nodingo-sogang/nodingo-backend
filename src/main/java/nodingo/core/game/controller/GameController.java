package nodingo.core.game.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import nodingo.core.game.dto.response.GameProfileResponse;
import nodingo.core.game.dto.result.GameProfileResult;
import nodingo.core.game.service.query.GameQueryService;
import nodingo.core.global.annotation.RequireOnboardingCompleted;
import nodingo.core.global.auth.CustomOAuth2User;
import nodingo.core.global.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Game", description = "유저 게임화(레벨/경험치/티어) API")
@RequireOnboardingCompleted
@RestController
@RequestMapping("/api/users/game")
@RequiredArgsConstructor
public class GameController {

    private final GameQueryService gameQueryService;

    @Operation(
            summary = "내 게임 프로필 조회",
            description = "유저의 현재 레벨, XP, 티어 및 일일 미션 달성 현황을 조회합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "게임 프로필을 성공적으로 조회했습니다.")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<GameProfileResponse>> getMyGameProfile(
            @AuthenticationPrincipal CustomOAuth2User user) {

        GameProfileResult result = gameQueryService.getMyProfile(user.getUser().getId());
        return ResponseEntity.ok(new ApiResponse<>(true, 200, "게임 프로필을 성공적으로 조회했습니다.", GameProfileResponse.from(result)));
    }
}