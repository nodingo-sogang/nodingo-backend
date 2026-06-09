package nodingo.core.global.util;

import java.time.LocalDate;
import java.time.LocalTime;

public class DateUtil {

    private DateUtil() {}

    public static LocalDate getNow() {
        return LocalDate.now();
    }

    public static LocalDate getTargetDate() {
        return LocalTime.now().isBefore(LocalTime.of(5, 0))
                ? LocalDate.now().minusDays(1)
                : LocalDate.now();
    }

    public static LocalDate getMinusOneDay() {
        return LocalDate.now().minusDays(1);
    }
}