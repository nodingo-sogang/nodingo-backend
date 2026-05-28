package nodingo.core.global.exception;

import nodingo.core.global.dto.response.ApiResponse;
import nodingo.core.global.exception.ai.AiIntegrationException;
import nodingo.core.global.exception.auth.InvalidTokenException;
import nodingo.core.global.exception.auth.TokenNotFoundException;
import nodingo.core.global.exception.keyword.KeywordNotFoundException;
import nodingo.core.global.exception.news.NewsIllegalException;
import nodingo.core.global.exception.news.NewsNotFoundException;
import nodingo.core.global.exception.quiz.QuizNotFoundException;
import nodingo.core.global.exception.recommendKeyword.RecommendKeywordNotFoundException;
import nodingo.core.global.exception.user.OnboardingNotCompletedException;
import nodingo.core.global.exception.user.UserNotFoundException;
import nodingo.core.global.exception.scrap.DuplicateScrapException;
import nodingo.core.global.exception.scrap.ScrapNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 유효성 검증 실패 (DTO Validation)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<List<String>>> handleValidationExceptions(MethodArgumentNotValidException e) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "입력 값이 유효하지 않습니다.", getErrorFields(e));
    }

    //OnboardingNotCompletedException
    @ExceptionHandler(OnboardingNotCompletedException.class)
    public ResponseEntity<ApiResponse<Void>> handleOnboardingNotCompleted(OnboardingNotCompletedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse<>(false, 403, e.getMessage(), null));
    }

    //InvalidTokenException
    @ExceptionHandler(InvalidTokenException.class)
    protected ResponseEntity<ApiResponse<?>> handleInvalidTokenException(InvalidTokenException e) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    //TokenNotFoundException
    @ExceptionHandler(TokenNotFoundException.class)
    protected ResponseEntity<ApiResponse<?>> handleTokenNotFoundException(TokenNotFoundException e) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, e.getMessage());
    }

    //UserNotFoundException
    @ExceptionHandler(UserNotFoundException.class)
    protected ResponseEntity<ApiResponse<?>> handleUserNotFoundException(UserNotFoundException e) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, e.getMessage());
    }

    //KeywordNotFoundException
    @ExceptionHandler(KeywordNotFoundException.class)
    protected ResponseEntity<ApiResponse<?>> handleKeywordNotFoundException(KeywordNotFoundException e) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, e.getMessage());
    }

    //RecommendKeywordNotFoundException
    @ExceptionHandler(RecommendKeywordNotFoundException.class)
    protected ResponseEntity<ApiResponse<?>> handleRecommendKeywordNotFoundException(RecommendKeywordNotFoundException e) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, e.getMessage());
    }

    //NewsNotFoundException
    @ExceptionHandler(NewsNotFoundException.class)
    protected ResponseEntity<ApiResponse<?>> handleNewsNotFoundException(NewsNotFoundException e) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, e.getMessage());
    }

    //UserScrapNotFoundException
    @ExceptionHandler(ScrapNotFoundException.class)
    protected ResponseEntity<ApiResponse<?>> handleUserScrapNotFoundException(ScrapNotFoundException e) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, e.getMessage());
    }

    //QuizNotFoundException
    @ExceptionHandler(QuizNotFoundException.class)
    protected ResponseEntity<ApiResponse<?>> handleQuizNotFoundException(QuizNotFoundException e) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, e.getMessage());
    }

    //NewsIllegalException
    @ExceptionHandler(NewsIllegalException.class)
    protected ResponseEntity<ApiResponse<?>> handleNewsIllegalException(NewsIllegalException e) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(DuplicateScrapException.class)
    protected ResponseEntity<ApiResponse<?>> handleDuplicateScrapException(DuplicateScrapException e){
        return buildErrorResponse(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(AiIntegrationException.class)
    public ResponseEntity<ApiResponse<?>> handleAiIntegrationException(AiIntegrationException e) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "AI server processing failed: " + e.getMessage());
    }


    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ApiResponse<?>> handleException(Exception e) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다: " + e.getMessage());
    }

    private ResponseEntity<ApiResponse<?>> buildErrorResponse(HttpStatus status, String message) {
        ApiResponse<?> response = new ApiResponse<>(false, status.value(), message);
        return ResponseEntity.status(status).body(response);
    }

    private <T> ResponseEntity<ApiResponse<T>> buildErrorResponse(HttpStatus status, String message, T data) {
        ApiResponse<T> response = new ApiResponse<>(false, status.value(), message, data);
        return ResponseEntity.status(status).body(response);
    }

    private static List<String> getErrorFields(MethodArgumentNotValidException e) {
        return e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.toList());
    }
}