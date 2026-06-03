package nodingo.core.quiz.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nodingo.core.global.annotation.RequireOnboardingCompleted;
import nodingo.core.global.auth.CustomOAuth2User;
import nodingo.core.global.dto.response.ApiResponse;
import nodingo.core.quiz.dto.command.QuizSubmitCommand;
import nodingo.core.quiz.dto.request.QuizSubmitRequest;
import nodingo.core.quiz.dto.response.QuizListResponse;
import nodingo.core.quiz.dto.response.QuizRewardResponse;
import nodingo.core.quiz.dto.result.QuizListResult;
import nodingo.core.quiz.dto.result.QuizRewardResult;
import nodingo.core.quiz.service.command.QuizService;
import nodingo.core.quiz.service.query.QuizQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Quiz", description = "노드 관련 퀴즈 조회 및 제출(채점) API")
@RequireOnboardingCompleted
@RestController
@RequestMapping("/api/graphs/nodes/{keywordId}/quizzes")
@RequiredArgsConstructor
public class QuizController {

    private final QuizQueryService quizQueryService;
    private final QuizService quizService;

    @Operation(
            summary = "퀴즈 목록 조회",
            description = "특정 노드와 관련된 퀴즈 3개를 조회합니다. 각 퀴즈별로 현재 유저의 풀이 여부(solved)를 함께 반환합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "퀴즈 목록을 성공적으로 조회했습니다.")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<QuizListResponse>> getQuizzes(
            @AuthenticationPrincipal CustomOAuth2User customOAuth2User,
            @PathVariable Long keywordId) {
        QuizListResult result = quizQueryService.getQuizzesByKeyword(customOAuth2User.getUser().getId(), keywordId);
        return ResponseEntity.ok(new ApiResponse<>(true, 200, "퀴즈 목록을 성공적으로 조회했습니다.", QuizListResponse.from(result)));
    }

    @Operation(
            summary = "퀴즈 정답 제출 및 보상 획득",
            description = "퀴즈를 채점하고 정답일 경우 XP와 보상을 반환합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "퀴즈 결과가 성공적으로 처리되었습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "이미 제출한 퀴즈입니다.")
    })
    @PostMapping("/{quizId}/submit")
    public ResponseEntity<ApiResponse<QuizRewardResponse>> submitQuiz(
            @PathVariable Long keywordId,
            @PathVariable Long quizId,
            @Valid @RequestBody QuizSubmitRequest request,
            @AuthenticationPrincipal CustomOAuth2User user) {
        QuizSubmitCommand command = QuizSubmitCommand.of(user.getUser().getId(), quizId, request);
        QuizRewardResult result = quizService.submitQuiz(command);
        return ResponseEntity.ok(new ApiResponse<>(true, 200, "퀴즈 결과가 성공적으로 처리되었습니다.", QuizRewardResponse.from(result)));
    }
}