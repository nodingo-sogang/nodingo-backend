package nodingo.core.keyword.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "keyword_alias",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"alias", "keyword_id"})
        },
        indexes = {
                @Index(name = "idx_alias", columnList = "alias")
        }
)
public class KeywordAlias {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "keyword_id", nullable = false)
    private Keyword keyword;

    @Column(nullable = false, length = 100)
    private String alias;

    public static KeywordAlias create(Keyword keyword, String alias) {
        KeywordAlias ka = new KeywordAlias();
        ka.keyword = keyword;
        ka.alias = normalize(alias);
        return ka;
    }

    private static String normalize(String input) {
        return input.toLowerCase().trim();
    }
}
