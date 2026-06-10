package nodingo.core.user.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nodingo.core.global.domain.BaseTimeEntity;
import nodingo.core.keyword.domain.Keyword;
import nodingo.core.keyword.domain.RecommendKeyword;
import nodingo.core.news.domain.News;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "user_scraps",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "news_id"}),
                @UniqueConstraint(columnNames = {"user_id", "keyword_id"})
        }
)
public class UserScrap extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "news_id")
    private News news;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "keyword_id")
    private Keyword keyword;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommend_keyword_id")
    private RecommendKeyword recommendKeyword;

    public static UserScrap createNewsScrap(User user, News news) {
        UserScrap scrap = new UserScrap();
        scrap.user = user;
        scrap.news = news;
        return scrap;
    }

    public static UserScrap createRecommendKeywordScrap(User user, RecommendKeyword recommendKeyword) {
        UserScrap scrap = new UserScrap();
        scrap.user = user;
        scrap.keyword = recommendKeyword.getKeyword();
        scrap.recommendKeyword = recommendKeyword;
        return scrap;
    }

    public static UserScrap createPureKeywordScrap(User user, Keyword keyword) {
        UserScrap scrap = new UserScrap();
        scrap.user = user;
        scrap.keyword = keyword;
        return scrap;
    }
}