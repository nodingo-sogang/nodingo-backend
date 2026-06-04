package nodingo.core.user.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import nodingo.core.global.domain.BaseTimeEntity;
import nodingo.core.keyword.domain.Keyword;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"provider", "providerId"})
        },
        indexes = {
                @Index(name = "idx_provider", columnList = "provider, providerId"),
                @Index(name = "idx_email", columnList = "email"),
                @Index(name = "idx_nickname", columnList = "nickname")
        }
)
public class User extends BaseTimeEntity implements UserDetails {

    private static final int EMBEDDING_DIMENSION = 1536;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false, unique = true)
    private String nickname;

    @Column(nullable = false)
    private String provider;

    @Column(nullable = false)
    private String providerId;

    private String naverAccessToken;

    private String refreshToken;

    @JdbcTypeCode(SqlTypes.VECTOR)
    @Column(columnDefinition = "vector(1536)")
    private float[] embedding;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "user_personas", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "persona")
    private List<UserPersona> personas = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserInterest> interests = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OnboardingStatus onboardingStatus = OnboardingStatus.PENDING;

    // ==========================================
    // Gamification (게임화) 통계 및 레벨 필드
    // ==========================================

    @Column(nullable = false, columnDefinition = "integer default 1")
    private Integer level = 1;

    @Column(nullable = false, columnDefinition = "integer default 0")
    private Integer xp = 0;

    @Column(nullable = false, columnDefinition = "integer default 0")
    private int weeklyXp = 0;

    @Column(nullable = false, columnDefinition = "integer default 0")
    private int totalNodesExplored = 0;

    @Column(nullable = false, columnDefinition = "integer default 0")
    private int totalKeywordsScrapped = 0;

    @Column(nullable = false, columnDefinition = "integer default 0")
    private int totalQuizzesCompleted = 0;

    private LocalDate lastVisitDate;

    @Column(nullable = false, columnDefinition = "integer default 0")
    private int consecutiveAttendanceDays = 0;

    private LocalDate lastQuizDate;

    @Column(nullable = false, columnDefinition = "integer default 0")
    private int dailyQuizzesCompleted = 0;

    // ==========================================
    // Spring Security UserDetails 구현
    // ==========================================

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override public String getPassword() { return null; }
    @Override public String getUsername() { return this.username; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }

    // ==========================================
    // 팩토리 및 비즈니스 로직
    // ==========================================

    public static User create(String provider, String providerId, String username, String name, String email, String nickname) {
        User user = new User();
        user.provider = provider;
        user.providerId = providerId;
        user.username = username;
        user.name = name;
        user.email = email;
        user.nickname = nickname;
        user.embedding = emptyEmbedding();
        return user;
    }

    public void updateEmbedding(float[] embedding) {
        this.embedding = embedding != null ? embedding : emptyEmbedding();
    }

    public void updateInfo(String name, String email) {
        this.name = name;
        this.email = email;
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void updateNaverAccessToken(String naverAccessToken) {
        this.naverAccessToken = naverAccessToken;
    }

    public void updateRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public void completeOnboarding(List<UserPersona> personas) {
        if (personas == null || personas.isEmpty()) {
            throw new IllegalArgumentException("At least one persona must be selected.");
        }
        if (personas.size() > 1) {
            throw new IllegalArgumentException("Only one persona can be selected.");
        }
        this.personas = new ArrayList<>(personas);
    }

    public void updateOnboardingStatus(OnboardingStatus status) {
        this.onboardingStatus = status;
    }

    public boolean isOnboardingCompleted() {
        return this.onboardingStatus == OnboardingStatus.COMPLETED;
    }

    public UserInterest addInterest(Keyword keyword, InterestLevel level, UserInterest parent, LocalDate targetDate) {
        return UserInterest.create(this, keyword, level, parent, targetDate);
    }

    public List<UserInterest> getInterests() {
        return Collections.unmodifiableList(interests);
    }

    protected List<UserInterest> getInterestsInternal() {
        return interests;
    }

    // ==========================================
    // Gamification 비즈니스 로직
    // ==========================================

    public boolean addXp(int earnedXp) {
        this.xp += earnedXp;
        this.weeklyXp += earnedXp;
        boolean isLevelUp = false;

        while (true) {
            int requiredXp = (int) Math.floor(100 * Math.pow(1.4, this.level - 1));
            if (this.xp >= requiredXp) {
                this.xp -= requiredXp;
                this.level++;
                isLevelUp = true;
            } else {
                break;
            }
        }
        return isLevelUp;
    }

    public void removeXp(int lostXp) {
        this.xp = Math.max(0, this.xp - lostXp);
        this.weeklyXp = Math.max(0, this.weeklyXp - lostXp);
    }

    public void addKeywordScrap() {
        this.totalKeywordsScrapped++;
    }

    public void removeKeywordScrap() {
        this.totalKeywordsScrapped = Math.max(0, this.totalKeywordsScrapped - 1);
    }

    public void addNodeExplore() {
        this.totalNodesExplored++;
    }

    public boolean checkAndAddDailyQuiz(LocalDate standardToday, int dailyGoalCount) {
        if (this.lastQuizDate == null || !standardToday.equals(this.lastQuizDate)) {
            this.dailyQuizzesCompleted = 0;
            this.lastQuizDate = standardToday;
        }
        this.dailyQuizzesCompleted++;
        this.totalQuizzesCompleted++;
        return this.dailyQuizzesCompleted == dailyGoalCount;
    }

    public boolean recordAttendance(LocalDate standardToday) {
        if (standardToday.equals(this.lastVisitDate)) {
            return false;
        }
        if (this.lastVisitDate != null && standardToday.minusDays(1).equals(this.lastVisitDate)) {
            this.consecutiveAttendanceDays++;
        } else {
            this.consecutiveAttendanceDays = 1;
        }
        this.lastVisitDate = standardToday;
        return true;
    }

    @Transient
    public String getTier() {
        if (this.level <= 3) return "새내기";
        if (this.level <= 7) return "시사 입문러";
        if (this.level <= 12) return "벼락치기 취준생";
        if (this.level <= 18) return "여의도 주니어";
        if (this.level <= 24) return "시사 덕후";
        return "살아있는 위키";
    }

    @Transient
    public int getXpNeededForNextLevel() {
        return (int) Math.floor(100 * Math.pow(1.4, this.level - 1));
    }

    private static float[] emptyEmbedding() {
        return new float[EMBEDDING_DIMENSION];
    }
}