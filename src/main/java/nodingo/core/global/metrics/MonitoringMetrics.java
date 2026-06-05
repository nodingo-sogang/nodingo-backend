package nodingo.core.global.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MonitoringMetrics {

    private final MeterRegistry registry;

    public void recordSchedulerSuccess(String jobName) {
        registry.counter("scheduler.job.success", "job", jobName).increment();
    }

    public void recordSchedulerFailure(String jobName) {
        registry.counter("scheduler.job.failure", "job", jobName).increment();
    }

    public Timer.Sample startTimer() {
        return Timer.start(registry);
    }

    public void stopTimer(Timer.Sample sample, String jobName, String status) {
        sample.stop(Timer.builder("scheduler.job.duration")
                .tag("job", jobName)
                .tag("status", status)
                .register(registry));
    }

    public void recordAiCall(String feature) {
        registry.counter("ai.call.total", "feature", feature).increment();
    }

    public void recordAiFailure(String feature, String errorType) {
        registry.counter("ai.call.failure",
                "feature", feature,
                "error", errorType).increment();
    }
}