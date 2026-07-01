package nodingo.core.user.repository.custom;

import nodingo.core.user.domain.User;
import nodingo.core.user.domain.UserPersona;

import java.util.List;
public interface UserRepositoryCustom {
    List<User> fetchLeaderboardByPersona(UserPersona persona, int limit, int offset);
    long countHigherRankedByPersona(UserPersona persona, int weeklyXp);
}