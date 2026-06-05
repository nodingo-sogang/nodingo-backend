package nodingo.core.batch.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import nodingo.core.ai.dto.newsBatch.NewsBatch;
import nodingo.core.batch.service.query.NewsBatchQueryService;
import nodingo.core.news.scheduler.NewsScheduler;
import nodingo.core.global.dto.response.ApiResponse;
import nodingo.core.notification.scheduler.NotificationScheduler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher; // 🌟 추가됨
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Tag(name = "Admin - Batch", description = "관리자용 배치 실행 API")
@RestController
@RequestMapping("/api/batch")
@RequiredArgsConstructor
public class BatchController {

    private final JobLauncher jobLauncher;
    private final Job dailyNewsJob;
    private final Job hourlyNotificationJob;
    private final NewsBatchQueryService newsBatchQueryService;

    @Operation(
            summary = "뉴스 수집 배치 수동 실행",
            description = "새벽 5시 스케줄러와 별개로, 즉시 뉴스 데이터를 수집하고 요약하는 배치를 실행합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "배치 실행 성공")
    })
    @SecurityRequirements(value = {})
    @PostMapping("/news-collect")
    public ResponseEntity<ApiResponse<Void>> triggerNewsJob() {
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("runId", UUID.randomUUID().toString())
                    .toJobParameters();

            jobLauncher.run(dailyNewsJob, jobParameters);

            return ResponseEntity.ok(new ApiResponse<>(true, 200, "뉴스 수집 배치(병렬 최적화 버전)가 성공적으로 트리거되었습니다.", null));
        } catch (Exception e) {
            log.error("News Batch Trigger Error: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, 500, "배치 실행 중 오류가 발생했습니다", null));
        }
    }

    @Operation(
            summary = "[파이썬 협업용] 뉴스 분석 요청 JSON 스펙 조회 (Real DB)",
            description = "실제 DB에 있는 최신 뉴스 100개를 바탕으로 파이썬 AI 서버로 던지는 Request JSON 구조(Chunk 100개 + 빈 키워드 배열)를 그대로 반환합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Mock 데이터 반환 성공")
    })
    @SecurityRequirements(value = {})
    @GetMapping("/news-request-spec")
    public ResponseEntity<ApiResponse<NewsBatch.Request>> getNewsRequestSpec() {
        try {
            NewsBatch.Request realRequest = newsBatchQueryService.getRealNewsRequestJson();
            return ResponseEntity.ok(new ApiResponse<>(true, 200, "실제 배치와 100% 동일한 JSON 스펙입니다.", realRequest));
        } catch (Exception e) {
            log.error("Mock Request Generate Error: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponse<>(false, 500, "JSON 스펙 생성 중 오류가 발생했습니다", null));
        }
    }

    @Operation(
            summary = "시간당 알림 배치 수동 실행",
            description = "매시간 정각 스케줄러와 별개로, '현재 시간'을 기준으로 알림 대상자를 찾아 즉시 FCM을 발송합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "알림 배치 실행 성공")
    })
    @SecurityRequirements(value = {})
    @PostMapping("/notification-push")
    public ResponseEntity<ApiResponse<Void>> triggerNotificationJob() {
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLocalDateTime("requestTime", LocalDateTime.now())
                    .toJobParameters();

            jobLauncher.run(hourlyNotificationJob, jobParameters);
            return ResponseEntity.ok(new ApiResponse<>(true, 200, "알림 발송 배치가 성공적으로 트리거되었습니다.", null));
        } catch (Exception e) {
            log.error("Notification Batch Trigger Error: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, 500, "알림 배치 실행 중 오류가 발생했습니다", null));
        }
    }
}