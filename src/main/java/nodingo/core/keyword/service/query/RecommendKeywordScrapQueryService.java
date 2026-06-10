package nodingo.core.keyword.service.query;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import nodingo.core.global.util.SliceUtil;
import nodingo.core.graph.dto.result.NodeSummaryResult;
import nodingo.core.keyword.domain.KeywordRelation;
import nodingo.core.keyword.dto.response.ScrapGraphResult;
import nodingo.core.keyword.dto.result.ScrapKeywordNodeResult;
import nodingo.core.keyword.repository.KeywordRelationRepository;
import nodingo.core.user.domain.UserScrap;
import nodingo.core.user.repository.UserScrapRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.stream.Collectors;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendKeywordScrapQueryService {

    private final UserScrapRepository userScrapRepository;
    private final KeywordRelationRepository keywordRelationRepository;

    public Slice<ScrapKeywordNodeResult> getScrapKeywordNodes(Long userId, int page) {
        Pageable pageable = PageRequest.of(page, 20);
        List<UserScrap> scraps = userScrapRepository.findKeywordScrapsByUserId(userId, page, 20);
        log.info(">>>> [Scrap Query] getScrapKeywordNodes. userId={}, page={}, count={}", userId, page, scraps.size());
        List<ScrapKeywordNodeResult> content = scraps.stream()
                .map(ScrapKeywordNodeResult::from)
                .collect(Collectors.toList());
        return SliceUtil.checkLastPage(pageable, content);
    }

    public Slice<NodeSummaryResult> getScrapKeywordSummaries(Long userId, int page) {
        Pageable pageable = PageRequest.of(page, 4);
        List<UserScrap> scraps = userScrapRepository.findKeywordScrapsByUserId(userId, page, 4);
        log.info(">>>> [Scrap Query] getScrapKeywordSummaries. userId={}, page={}, count={}", userId, page, scraps.size());
        List<NodeSummaryResult> content = scraps.stream()
                .map(scrap -> NodeSummaryResult.from(scrap.getRecommendKeyword()))
                .collect(Collectors.toList());
        return SliceUtil.checkLastPage(pageable, content);
    }

    public ScrapGraphResult getScrapKeywordGraph(Long userId) {
        log.info(">>>> [Scrap Query] getScrapKeywordGraph. userId={}", userId);

        List<UserScrap> scraps = userScrapRepository.findAllByUserId(userId);

        if (scraps.isEmpty()) {
            return new ScrapGraphResult(Collections.emptyList(), Collections.emptyList());
        }

        List<Long> scrapKeywordIds = scraps.stream()
                .map(s -> s.getRecommendKeyword().getKeyword().getId())
                .toList();

        List<ScrapGraphResult.NodeResult> nodes = scraps.stream()
                .map(s -> {
                    var rk = s.getRecommendKeyword();
                    var k = rk.getKeyword();
                    return new ScrapGraphResult.NodeResult(
                            k.getId(),
                            k.getWord(),
                            k.getPersona().name(),
                            rk.getScore()
                    );
                }).toList();

        List<KeywordRelation> relations = keywordRelationRepository.findAllRelationsIn(scrapKeywordIds);

        List<ScrapGraphResult.EdgeResult> edges = relations.stream()
                .map(r -> new ScrapGraphResult.EdgeResult(
                        r.getSubjectKeyword().getId(),
                        r.getRelatedKeyword().getId(),
                        r.getRelationScore()
                )).toList();

        return new ScrapGraphResult(nodes, edges);
    }
}