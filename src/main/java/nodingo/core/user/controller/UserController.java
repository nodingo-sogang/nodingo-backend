package nodingo.core.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nodingo.core.global.auth.CustomOAuth2User;
import nodingo.core.global.dto.response.ApiResponse;
import nodingo.core.user.domain.User;
import nodingo.core.user.domain.UserPersona;
import nodingo.core.user.dto.command.SaveOnboardingCommand;
import nodingo.core.user.dto.request.OnboardingRequest;
import nodingo.core.user.dto.response.KeywordListResponse;
import nodingo.core.user.dto.response.OnboardingStatusResponse;
import nodingo.core.user.dto.response.PersonaListResponse;
import nodingo.core.user.dto.response.UserProgressResponse;
import nodingo.core.user.dto.result.KeywordListResult;
import nodingo.core.user.dto.result.OnboardingStatusResult;
import nodingo.core.user.dto.result.PersonaListResult;
import nodingo.core.user.dto.result.UserProgressResult;
import nodingo.core.user.service.async.OnboardingAsyncService;
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
            summary = "내 탐험 진행률 조회",
            description = "전체 노드 대비 유저가 탐험한 노드의 비율 및 카운트를 조회합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "진행률 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "온보딩 미완료 유저"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "유저 정보를 찾을 수 없음")
    })
    @GetMapping("/progress")
    public ResponseEntity<ApiResponse<UserProgressResponse>> getMyProgress(
            @AuthenticationPrincipal CustomOAuth2User user) {
        UserProgressResult result = userProgressQueryService.getMyProgress(user.getUser().getId());
        return ResponseEntity.ok(new ApiResponse<>(true, 200, "진행률 조회 성공", UserProgressResponse.from(result)));
    }
}