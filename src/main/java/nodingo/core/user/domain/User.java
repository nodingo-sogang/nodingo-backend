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
                @Index(name = "idx_email", columnList = "email")
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

    @Column(nullable = false)
    private String provider;

    @Column(nullable = false)
    private String providerId;

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

    @Column(unique = true, length = 20)
    private String inviteCode;

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
    // 팩토리 및 기존 비즈니스 로직
    // ==========================================

    public static User create(String provider, String providerId, String username, String name, String email) {
        User user = new User();
        user.provider = provider;
        user.providerId = providerId;
        user.username = username;
        user.name = name;
        user.email = email;
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

    public UserInterest addInterest(Keyword keyword,
                                    InterestLevel level,
                                    UserInterest parent,
                                    LocalDate targetDate) {
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

    /**
     * XP 획득 및 수학 공식 기반 레벨업 처리
     * @return 레벨업 여부
     */
    public boolean addXp(int earnedXp) {
        this.xp += earnedXp;
        boolean isLevelUp = false;

        while (true) {
            // 프론트 공식: xpForLevel(level) = Math.floor(100 * 1.4 ** (level - 1))
            int requiredXp = (int) Math.floor(100 * Math.pow(1.4, this.level - 1));

            // 현재 누적 XP가 요구 XP를 넘으면 레벨업 후 남은 XP 이월
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

    /**
     * XP 차감 (스크랩 취소 등) - 현재 레벨에서 XP가 0 미만으로 떨어져 강등되지는 않음
     */
    public void removeXp(int lostXp) {
        this.xp = Math.max(0, this.xp - lostXp);
    }

    /**
     * 스크랩 카운트 증가
     */
    public void addKeywordScrap() {
        this.totalKeywordsScrapped++;
    }

    /**
     * 스크랩 카운트 차감 (0 미만 방지)
     */
    public void removeKeywordScrap() {
        this.totalKeywordsScrapped = Math.max(0, this.totalKeywordsScrapped - 1);
    }

    public void addNodeExplore() {
        this.totalNodesExplored++;
    }

    /**
     * 🌟 일일 퀴즈 목표 달성 여부 체크 및 지연 초기화 트리거
     * @param standardToday 서비스 레이어에서 계산해서 넘겨준 새벽 5시 윈도우 기준 당일 날짜
     * @param dailyGoalCount 정책 파일에서 정의한 목표 퀴즈 개수 (현재 2개)
     * @return 목표 도달 시점에 딱 한 번만 true 반환 (영수증 보너스 팝업용)
     */
    public boolean checkAndAddDailyQuiz(LocalDate standardToday, int dailyGoalCount) {
        // 1. 마지막으로 퀴즈 푼 날짜가 새벽 5시 기준일보다 과거라면, 새로운 날이 밝았으므로 일일 카운터 리셋!
        if (this.lastQuizDate == null || !standardToday.equals(this.lastQuizDate)) {
            this.dailyQuizzesCompleted = 0;
            this.lastQuizDate = standardToday;
        }

        // 2. 카운터 증가
        this.dailyQuizzesCompleted++;
        this.totalQuizzesCompleted++;

        // 3. 🌟 드디어 quizPolicy.getDailyGoalCount()를 사용하여 하드코딩 없이 비즈니스 목표 달성 검증!
        return this.dailyQuizzesCompleted == dailyGoalCount;
    }

    /**
     * 🌟 [출석 동기화] 출석 트래킹 및 연속 출석 체크 (5시 기준일 정렬)
     * @param standardToday 서비스 레이어에서 계산해서 넘겨준 새벽 5시 윈도우 기준 당일 날짜
     * @return 오늘 새벽 5시 윈도우상 첫 출석이면 true (이때 출석 XP 지급)
     */
    public boolean recordAttendance(LocalDate standardToday) {
        if (standardToday.equals(this.lastVisitDate)) {
            return false; // 새벽 5시 기준 윈도우상 오늘 이미 방문 처리됨
        }

        // 바로 직전 5시 기준일 날짜와 대조하여 연속 출석일 증가 처리
        if (this.lastVisitDate != null && standardToday.minusDays(1).equals(this.lastVisitDate)) {
            this.consecutiveAttendanceDays++;
        } else {
            this.consecutiveAttendanceDays = 1; // 연속 출석 깨짐 또는 최초 유저
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

    // ==========================================
    // 친구 초대 비즈니스 로직
    // ==========================================

    public void assignInviteCode(String inviteCode) {
        if (this.inviteCode == null) {
            this.inviteCode = inviteCode;
        }
    }

    private static float[] emptyEmbedding() {
        return new float[EMBEDDING_DIMENSION];
    }
}