package nodingo.core.global.util;

import java.time.LocalDate;
import java.time.ZoneId;

public class BatchDateUtil {

    private BatchDateUtil() {}

    public static LocalDate getTargetDate() {
        return LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(1);
    }
}