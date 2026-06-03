package nodingo.core.user.service.query;

import lombok.RequiredArgsConstructor;
import nodingo.core.user.domain.BadgeType;
import nodingo.core.user.domain.UserBadge;
import nodingo.core.user.dto.result.BadgeInfoResult;
import nodingo.core.user.dto.result.BadgeListResult;
import nodingo.core.user.repository.UserBadgeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BadgeQueryService {

    private final UserBadgeRepository userBadgeRepository;

    public BadgeListResult getUserBadges(Long userId) {
        List<UserBadge> earnedBadges = userBadgeRepository.findAllByUserId(userId);

        Map<BadgeType, UserBadge> earnedMap = earnedBadges.stream()
                .collect(Collectors.toMap(UserBadge::getBadgeType, ub -> ub));

        List<BadgeInfoResult> list = Arrays.stream(BadgeType.values())
                .map(type -> BadgeInfoResult.of(type, earnedMap.get(type)))
                .toList();

        return new BadgeListResult(list);
    }
}
