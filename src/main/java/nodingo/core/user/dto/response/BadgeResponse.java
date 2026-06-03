package nodingo.core.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BadgeResponse {
    private String id;
    private String name;
    private String category;
    private String description;
    private String condition;
    private boolean earned;
    private LocalDate earnedAt;
}
