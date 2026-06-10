package nodingo.core.user.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import nodingo.core.user.domain.User;

@Getter
@Builder
@AllArgsConstructor
public class RankingEntryResult {
    private int rank;
    private Long userId;
    private String nickname;
    private int level;
    private int weekXp;
    private String persona;
    private boolean isMe;
    private String profileImageUrl;

    public static RankingEntryResult from(User user, int rank, Long myUserId) {
        String displayPersona = user.getPersonas().isEmpty() ? "NONE" : user.getPersonas().get(0).name();
        return RankingEntryResult.builder()
                .rank(rank)
                .userId(user.getId())
                .nickname(user.getNickname() != null ? user.getNickname() : user.getName())
                .level(user.getLevel())
                .weekXp(user.getWeeklyXp())
                .persona(displayPersona)
                .isMe(user.getId().equals(myUserId))
                .profileImageUrl(user.getProfileImageUrl())
                .build();
    }

    public static RankingEntryResult ofFallback(User user) {
        String displayPersona = user.getPersonas().isEmpty() ? "NONE" : user.getPersonas().get(0).name();
        return RankingEntryResult.builder()
                .rank(999)
                .userId(user.getId())
                .nickname(user.getNickname() != null ? user.getNickname() : user.getName())
                .level(user.getLevel())
                .weekXp(user.getWeeklyXp())
                .persona(displayPersona)
                .isMe(true)
                .profileImageUrl(user.getProfileImageUrl())  // 추가
                .build();
    }
}