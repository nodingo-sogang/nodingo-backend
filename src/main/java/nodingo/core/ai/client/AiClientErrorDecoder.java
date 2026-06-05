package nodingo.core.ai.client;

import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nodingo.core.global.exception.ai.AiRateLimitException;
import nodingo.core.global.metrics.MonitoringMetrics;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiClientErrorDecoder implements ErrorDecoder {

    private final MonitoringMetrics metrics;
    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        if (response.status() == 429) {
            log.error("[AI Client] OpenAI Rate Limit Exceeded (429) | method={}", methodKey);
            metrics.recordAiFailure(methodKey, "RateLimitError");
            return new AiRateLimitException("OpenAI 429 RateLimit 초과: " + methodKey);
        }
        if (response.status() >= 500) {
            log.error("[AI Client] AI Server Error | status={}, method={}", response.status(), methodKey);
            metrics.recordAiFailure(methodKey, "ServerError_" + response.status());
        }
        return defaultDecoder.decode(methodKey, response);
    }
}