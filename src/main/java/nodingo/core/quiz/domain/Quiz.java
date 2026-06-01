package nodingo.core.quiz.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nodingo.core.global.domain.BaseTimeEntity;
import nodingo.core.keyword.domain.Keyword;
import nodingo.core.news.domain.News;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "quizzes")
public class Quiz extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "keyword_id", nullable = false)
    private Keyword keyword;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "news_id")
    private News news;

    @Column(nullable = false, length = 500)
    private String question;

    private String option1;
    private String option2;
    private String option3;
    private String option4;

    @Column(nullable = false)
    private Integer answerIndex;

    public static Quiz create(Keyword keyword, News news, String question,
                              String option1, String option2, String option3, String option4,
                              Integer answerIndex) {
        Quiz quiz = new Quiz();
        quiz.keyword = keyword;
        quiz.news = news;
        quiz.question = question;
        quiz.option1 = option1;
        quiz.option2 = option2;
        quiz.option3 = option3;
        quiz.option4 = option4;
        quiz.answerIndex = answerIndex;
        return quiz;
    }
}