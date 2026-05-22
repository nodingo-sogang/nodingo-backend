package nodingo.core.keyword.repository.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import nodingo.core.keyword.domain.Keyword;
import nodingo.core.keyword.dto.query.KeywordCandidate;
import nodingo.core.keyword.dto.query.QKeywordCandidate;
import nodingo.core.keyword.repository.custom.KeywordRepositoryCustom;
import nodingo.core.user.domain.InterestLevel;
import nodingo.core.user.domain.UserPersona;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static nodingo.core.keyword.domain.QKeyword.keyword;
import static nodingo.core.keyword.domain.QNewsKeyword.newsKeyword;
import static nodingo.core.news.domain.QNews.news;

@RequiredArgsConstructor
public class KeywordRepositoryImpl implements KeywordRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<KeywordCandidate> findCandidateKeywords(LocalDateTime startTime, LocalDateTime endTime) {
        return queryFactory
                .select(new QKeywordCandidate(
                        keyword.id,
                        keyword.word,
                        keyword.embedding
                ))
                .distinct()
                .from(keyword)
                .join(keyword.newsKeywords, newsKeyword)
                .join(newsKeyword.news, news)
                .where(
                        news.dateTimePub.between(startTime, endTime)
                )
                .fetch();
    }

    @Override
    public List<Keyword> findMacrosByDate(Collection<String> words, LocalDate targetDate) {
        return queryFactory
                .selectFrom(keyword)
                .where(
                        keyword.word.in(words),
                        keyword.level.eq(InterestLevel.MACRO),
                        keyword.targetDate.eq(targetDate)
                )
                .fetch();
    }

    @Override
    public List<Keyword> findSpecificsByDate(Collection<String> normalizedWords, LocalDate targetDate) {
        return queryFactory
                .selectFrom(keyword)
                .where(
                        keyword.normalizedWord.in(normalizedWords),
                        keyword.level.eq(InterestLevel.SPECIFIC),
                        keyword.targetDate.eq(targetDate)
                )
                .fetch();
    }

    @Override
    public List<Keyword> findMacrosForOnboarding(UserPersona persona, LocalDate targetDate, int limit) {
        return queryFactory
                .selectFrom(keyword)
                .where(
                        keyword.persona.eq(persona),
                        keyword.level.eq(InterestLevel.MACRO),
                        keyword.targetDate.eq(targetDate)
                )
                .limit(limit)
                .fetch();
    }

    @Override
    public List<Keyword> findSpecificsForOnboarding(Long macroId, LocalDate targetDate, int limit) {
        return queryFactory
                .selectFrom(keyword)
                .where(
                        keyword.parent.id.eq(macroId),
                        keyword.level.eq(InterestLevel.SPECIFIC),
                        keyword.targetDate.eq(targetDate)
                )
                .limit(limit)
                .fetch();
    }
}
