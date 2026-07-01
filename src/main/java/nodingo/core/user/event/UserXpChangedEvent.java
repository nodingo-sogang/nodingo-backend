package nodingo.core.user.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import nodingo.core.user.domain.User;

@Getter
@AllArgsConstructor
public class UserXpChangedEvent {
    private User user;
}