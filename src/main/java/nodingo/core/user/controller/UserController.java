package nodingo.core.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nodingo.core.user.dto.response.*;
import nodingo.core.user.dto.result.*;
import nodingo.core.global.annotation.RequireOnboardingCompleted;
import nodingo.core.global.auth.CustomOAuth2User;
import nodingo.core.global.dto.response.ApiResponse;
import nodingo.core.user.domain.User;
import nodingo.core.user.domain.UserPersona;
import nodingo.core.user.dto.command.SaveOnboardingCommand;
import nodingo.core.user.dto.request.OnboardingRequest;
import nodingo.core.user.service.async.OnboardingAsyncService;
import nodingo.core.user.service.command.UserGameService;
import nodingo.core.user.service.query.BadgeQueryService;
import nodingo.core.user.service.query.GameQueryService;
import nodingo.core.user.service.query.OnboardingQueryService;
import nodingo.core.user.service.command.OnboardingService;
import nodingo.core.user.service.query.UserProgressQueryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "User", description = "사용자 관련 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final OnboardingService onboardingService;
    private final OnboardingAsyncService onboardingAsyncService;
    private final OnboardingQueryService onboardingQueryService;
    private final UserProgressQueryService userProgressQueryService;
    private final UserGameService userGameService;
    private final GameQueryService gameQueryService;
    private final BadgeQueryService badgeQueryService;

    @Operation(
            summary = "대분류(Persona) 목록 조회",
            description = "온보딩 시작 시 선택 가능한 페르소나(대분류) 목록을 조회합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공적으로 페르소나 목록을 조회했습니다.")
    })
    @GetMapping("/keywords/personas")
    public ResponseEntity<ApiResponse<PersonaListResponse>> getPersonas() {
        PersonaListResult result = onboardingQueryService.getPersonas();
        return ResponseEntity.ok(new ApiResponse<>(true, 200, "성공적으로 유저 맞춤형 페르소나 목록을 조회했습니다.", PersonaListResponse.from(result)));
    }

    @Operation(
            summary = "중분류(Macro) 목록 조회",
            description = "선택한 페르소나에 해당하는 중분류 키워드 목록을 조회합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공적으로 중분류 목록을 조회했습니다.")
    })
    @GetMapping("/keywords/macro")
    public ResponseEntity<ApiResponse<KeywordListResponse>> getMacros(
            @RequestParam UserPersona persona) {
        KeywordListResult result = onboardingQueryService.getMacroKeywords(persona);
        return ResponseEntity.ok(new ApiResponse<>(true, 200, "성공적으로 유저 맞춤형 중분류 목록을 조회했습니다.",KeywordListResponse.from(result)));
    }

    @Operation(
            summary = "소분류(Specific) 목록 조회",
            description = "선택한 중분류 ID 하위의 소분류 키워드 목록을 조회합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공적으로 소분류 목록을 조회했습니다.")
    })
    @GetMapping("/keywords/specific")
    public ResponseEntity<ApiResponse<KeywordListResponse>> getSpecifics(
            @RequestParam Long macroId) {
        KeywordListResult result = onboardingQueryService.getSpecificKeywords(macroId);
        return ResponseEntity.ok(new ApiResponse<>(true, 200, "성공적으로 유저 맞춤형 소분류 목록을 조회했습니다.", KeywordListResponse.from(result)));
    }

    @Operation(
            summary = "유저 온보딩 관심사 설정",
            description = "인증된 유저 정보를 바탕으로 페르소나 및 관심 키워드를 저장합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "성공적으로 온보딩 관심사 설정을 완료했습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @PostMapping("/onboarding")
    public ResponseEntity<ApiResponse<Void>> completeOnboarding(
            @AuthenticationPrincipal CustomOAuth2User customOAuth2User,
            @Valid @RequestBody OnboardingRequest request) {
        User loginUser = customOAuth2User.getUser();
        SaveOnboardingCommand command = SaveOnboardingCommand.from(loginUser.getId(), request);
        List<Long> keywordIds = onboardingService.saveOnboardingInfo(command);
        onboardingAsyncService.initEmbeddingAndRecommend(loginUser.getId(), keywordIds);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new ApiResponse<>(true, 202, "성공적으로 온보딩 관심사 설정을 요청하였습니다."));
    }

    @Operation(
            summary = "온보딩 상태 조회",
            description = "온보딩 처리 상태를 조회합니다. PENDING/COMPLETED/FAILED"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공적으로 온보딩 상태를 조회했습니다.")
    })
    @GetMapping("/onboarding/status")
    public ResponseEntity<ApiResponse<OnboardingStatusResponse>> getOnboardingStatus(
            @AuthenticationPrincipal CustomOAuth2User customOAuth2User) {
        OnboardingStatusResult result = onboardingQueryService.getOnboardingStatus(customOAuth2User.getUser().getId());
        return ResponseEntity.ok(new ApiResponse<>(true, 200, "성공적으로 온보딩 상태를 조회했습니다.", OnboardingStatusResponse.from(result)));
    }

    @Operation(
            summary = "내 탐험 진행률 조회 (및 출석 체크)",
            description = "전체 노드 대비 유저가 탐험한 노드의 비율을 조회합니다. 오늘 첫 호출 시 출석 보상이 지급됩니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "진행률 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "온보딩 미완료 유저"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "유저 정보를 찾을 수 없음")
    })
    @RequireOnboardingCompleted
    @GetMapping("/progress")
    public ResponseEntity<ApiResponse<UserProgressResponse>> getMyProgress(
            @AuthenticationPrincipal CustomOAuth2User user) {
        Long userId = user.getUser().getId();
        userGameService.checkAndRewardAttendance(userId);
        UserProgressResult result = userProgressQueryService.getMyProgress(userId);
        return ResponseEntity.ok(new ApiResponse<>(true, 200, "진행률 조회 성공", UserProgressResponse.from(result)));
    }

    @Operation(
            summary = "내 게임 프로필 조회",
            description = "유저의 현재 레벨, XP, 티어 및 일일 미션 달성 현황을 조회합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "게임 프로필을 성공적으로 조회했습니다.")
    })
    @RequireOnboardingCompleted
    @GetMapping("/game")
    public ResponseEntity<ApiResponse<GameProfileResponse>> getMyGameProfile(
            @AuthenticationPrincipal CustomOAuth2User user) {

        GameProfileResult result = gameQueryService.getMyProfile(user.getUser().getId());
        return ResponseEntity.ok(new ApiResponse<>(true, 200, "게임 프로필을 성공적으로 조회했습니다.", GameProfileResponse.from(result)));
    }

    @Operation(
            summary = "내 뱃지 그리드 목록 조회",
            description = "획득/미획득 상태인 모든 종류의 뱃지 리스트를 총합 반환합니다. 미획득 상태인 뱃지는 earned=false, earned_at=null로 가이드됩니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "뱃지 목록을 완벽하게 불러왔습니다.")
    })
    @RequireOnboardingCompleted
    @GetMapping("/badges")
    public ResponseEntity<ApiResponse<BadgeListResponse>> getMyBadges(
            @AuthenticationPrincipal CustomOAuth2User customOAuth2User) {
        BadgeListResult result = badgeQueryService.getUserBadges(customOAuth2User.getUser().getId());
        return ResponseEntity.ok(new ApiResponse<>(true, 200, "뱃지 목록을 성공적으로 조회했습니다.", BadgeListResponse.from(result)));
    }
}