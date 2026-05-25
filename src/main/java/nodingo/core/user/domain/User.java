package nodingo.core.user.domain;

import jakarta.persistence.*;
import lombok.*;
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

    private static float[] emptyEmbedding() {
        return new float[EMBEDDING_DIMENSION];
    }
}