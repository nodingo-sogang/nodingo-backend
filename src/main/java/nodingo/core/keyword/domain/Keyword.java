package nodingo.core.keyword.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nodingo.core.global.domain.BaseTimeEntity;
import nodingo.core.user.domain.InterestLevel;
import nodingo.core.user.domain.UserPersona;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "keywords",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_keyword_normalized_word_level_date",
                        columnNames = {"normalized_word", "level", "target_date"}
                )
        }
)
public class Keyword extends BaseTimeEntity {

    private static final int EMBEDDING_DIMENSION = 1536;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String word;

    @Column(nullable = false, length = 100)
    private String normalizedWord;

    @JdbcTypeCode(SqlTypes.VECTOR)
    @Column(columnDefinition = "vector(1536)")
    private float[] embedding;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private UserPersona persona;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InterestLevel level;

    @Column(nullable = true)
    private LocalDate targetDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Keyword parent;

    @Column(nullable = false, columnDefinition = "integer default 1")
    private Integer unlockLevel = 1;

    @OneToMany(mappedBy = "keyword", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<KeywordAlias> aliases = new ArrayList<>();

    @OneToMany(mappedBy = "keyword")
    private List<NewsKeyword> newsKeywords = new ArrayList<>();

    public static Keyword createOnboardingKeyword(String word, UserPersona persona, InterestLevel level, Keyword parent) {
        Keyword keyword = new Keyword();
        keyword.word = word;
        keyword.normalizedWord = normalize(word);
        keyword.embedding = emptyEmbedding();
        keyword.persona = persona;
        keyword.level = level;
        keyword.parent = parent;
        keyword.addAlias(word);
        return keyword;
    }

    public static Keyword createMacro(String word, UserPersona persona, LocalDate targetDate) {
        Keyword keyword = new Keyword();
        keyword.word = word;
        keyword.normalizedWord = normalize(word);
        keyword.embedding = emptyEmbedding();
        keyword.persona = persona;
        keyword.level = InterestLevel.MACRO;
        keyword.targetDate = targetDate;
        keyword.addAlias(word);
        return keyword;
    }

    public static Keyword createSpecific(String word, UserPersona persona, Keyword parent, LocalDate targetDate) {
        Keyword keyword = new Keyword();
        keyword.word = word;
        keyword.normalizedWord = normalize(word);
        keyword.embedding = emptyEmbedding();
        keyword.persona = persona;
        keyword.level = InterestLevel.SPECIFIC;
        keyword.parent = parent;
        keyword.targetDate = targetDate;
        keyword.addAlias(word);
        return keyword;
    }

    public static Keyword create(String word, LocalDate targetDate) {
        Keyword keyword = new Keyword();
        keyword.word = word;
        keyword.normalizedWord = normalize(word);
        keyword.embedding = emptyEmbedding();
        keyword.level = InterestLevel.SPECIFIC;
        keyword.targetDate = targetDate;
        keyword.addAlias(word);
        return keyword;
    }

    public void updatePersonaAndParent(UserPersona persona, Keyword parent) {
        if (this.persona == null && persona != null) {
            this.persona = persona;
        }
        if (this.parent == null && parent != null) {
            this.parent = parent;
        }
    }

    public void updateEmbedding(float[] embedding) {
        this.embedding = embedding != null ? embedding : emptyEmbedding();
    }

    public void addAlias(String alias) {
        KeywordAlias ka = KeywordAlias.create(this, alias);
        this.aliases.add(ka);
    }

    private static String normalize(String input) {
        return input.toLowerCase().trim();
    }

    public void updateUnlockLevel(Integer unlockLevel) {
        if (unlockLevel != null) {
            this.unlockLevel = unlockLevel;
        }
    }

    private static float[] emptyEmbedding() {
        return new float[EMBEDDING_DIMENSION];
    }
}