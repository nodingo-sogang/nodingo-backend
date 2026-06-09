package nodingo.core.global.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

public class DateUtil {
    private static final LocalTime BATCH_COMPLETE_TIME = LocalTime.of(6, 0);

    public static LocalDate getTargetDate() {
        return LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(1);
    }

    public static LocalDate getApiTargetDate() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));

        if (now.toLocalTime().isBefore(BATCH_COMPLETE_TIME)) {
            return now.toLocalDate().minusDays(1);
        }
        return now.toLocalDate();
    }
}