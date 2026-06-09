package nodingo.core.global.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

public class DateUtil {

    private DateUtil() {}

    public static LocalDate getNow() {
        return LocalDate.now(ZoneId.of("Asia/Seoul"));
    }

    public static LocalDate getTargetDate() {
        return LocalTime.now(ZoneId.of("Asia/Seoul")).isBefore(LocalTime.of(5, 0))
                ? LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(1)
                : LocalDate.now(ZoneId.of("Asia/Seoul"));
    }

    public static LocalDate getMinusOneDay() {
        return LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(1);
    }
}