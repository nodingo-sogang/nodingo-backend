package nodingo.core.global.aop;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nodingo.core.global.auth.CustomOAuth2User;
import nodingo.core.global.exception.user.OnboardingNotCompletedException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OnboardingStatusAspect {

    @Around("@annotation(nodingo.core.global.annotation.RequireOnboardingCompleted) || @within(nodingo.core.global.annotation.RequireOnboardingCompleted)")
    public Object checkOnboardingStatus(ProceedingJoinPoint joinPoint) throws Throwable {
        log.info(">>>> [AOP] checkOnboardingStatus called. method={}", joinPoint.getSignature().getName());
        for (Object arg : joinPoint.getArgs()) {
            if (arg instanceof CustomOAuth2User customUser) {
                if (!customUser.getUser().isOnboardingCompleted()) {
                    throw new OnboardingNotCompletedException("온보딩이 완료되지 않았습니다.");
                }
                break;
            }
        }
        return joinPoint.proceed();
    }
}