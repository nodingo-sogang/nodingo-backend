package nodingo.core.global.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nodingo.core.global.auth.CustomOAuth2User;
import nodingo.core.global.auth.dto.request.ReissueTokenRequest;
import nodingo.core.global.auth.dto.response.ReissueTokenResponse;
import nodingo.core.global.auth.jwt.JwtConstants;
import nodingo.core.global.auth.service.AuthCommandService;
import nodingo.core.global.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "인증 (Auth)", description = "토큰 재발급, 로그아웃 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthCommandService authCommandService;

    @Operation(summary = "토큰 재발급 (Refresh)", description = "만료된 Access Token과 Refresh Token을 사용하여 새로운 토큰을 발급받습니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "재발급 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "유효하지 않은 토큰")
    })
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<ReissueTokenResponse>> refreshToken(
            @RequestHeader(value = JwtConstants.HEADER_STRING) String authHeader,
            @Valid @RequestBody ReissueTokenRequest request) {
        String accessToken = extractAccessToken(authHeader);
        ReissueTokenResponse response = authCommandService.reissue(accessToken, request.getRefreshToken());
        return ResponseEntity.ok(new ApiResponse<>(true, 200, "토큰이 재발급되었습니다.", response));
    }

    @Operation(summary = "로그아웃", description = "서버에서 리프레시 토큰을 삭제하고 해당 액세스 토큰을 무효화합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그아웃 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "유효하지 않은 토큰")
    })
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal CustomOAuth2User customOAuth2User,
            @RequestHeader(value = JwtConstants.HEADER_STRING) String authHeader) {
        String accessToken = extractAccessToken(authHeader);
        authCommandService.logout(customOAuth2User.getUser(), accessToken);
        return ResponseEntity.ok(new ApiResponse<>(true, 200, "성공적으로 로그아웃되었습니다.", null));
    }

    @Operation(summary = "회원 탈퇴", description = "네이버 연동 해제 및 본인 계정의 모든 데이터를 영구 삭제합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "탈퇴 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "유효하지 않은 토큰")
    })
    @DeleteMapping("/withdraw")
    public ResponseEntity<ApiResponse<Void>> withdraw(
            @AuthenticationPrincipal CustomOAuth2User customOAuth2User,
            @RequestHeader(value = JwtConstants.HEADER_STRING) String authHeader) {
        String accessToken = extractAccessToken(authHeader);
        authCommandService.withdraw(customOAuth2User.getUser(), accessToken);
        return ResponseEntity.ok(new ApiResponse<>(true, 200, "성공적으로 탈퇴 처리되었습니다.", null));
    }

    private String extractAccessToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}