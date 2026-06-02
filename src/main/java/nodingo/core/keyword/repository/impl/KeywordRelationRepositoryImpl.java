package nodingo.core.keyword.repository.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import nodingo.core.global.util.SliceUtil;
import nodingo.core.keyword.domain.KeywordRelation;
import nodingo.core.keyword.domain.QKeywordRelation;
import nodingo.core.keyword.repository.custom.KeywordRelationRepositoryCustom;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class KeywordRelationRepositoryImpl implements KeywordRelationRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Slice<KeywordRelation> findTopRelations(Long keywordId, Pageable pageable) {
        QKeywordRelation kr = QKeywordRelation.keywordRelation;

        List<KeywordRelation> content = queryFactory
                .selectFrom(kr)
                .where(
                        kr.subjectKeyword.id.eq(keywordId)
                                .or(kr.relatedKeyword.id.eq(keywordId))
                )
                .orderBy(kr.relationScore.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1)
                .fetch();

        return SliceUtil.checkLastPage(pageable, content);
    }

    @Override
    public List<KeywordRelation> findAllRelationsIn(List<Long> keywordIds) {
        QKeywordRelation kr = QKeywordRelation.keywordRelation;

        return queryFactory
                .selectFrom(kr)
                .where(
                        kr.subjectKeyword.id.in(keywordIds)
                                .and(kr.relatedKeyword.id.in(keywordIds))
                )
                .orderBy(kr.relationScore.desc())
                .fetch();
    }

    @Override
    public Optional<KeywordRelation> findByPair(Long subjectId, Long relatedId) {
        QKeywordRelation kr = QKeywordRelation.keywordRelation;

        return Optional.ofNullable(
                queryFactory
                        .selectFrom(kr)
                        .where(
                                kr.subjectKeyword.id.eq(subjectId)
                                        .and(kr.relatedKeyword.id.eq(relatedId))
                        )
                        .fetchOne()
        );
    }
}