package nodingo.core.keyword.service.query;

import lombok.RequiredArgsConstructor;
import nodingo.core.global.util.SliceUtil;
import nodingo.core.graph.dto.result.NodeSummaryResult;
import nodingo.core.keyword.dto.result.ScrapKeywordNodeResult;
import nodingo.core.user.domain.UserScrap;
import nodingo.core.user.repository.UserScrapRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendKeywordScrapQueryService {

    private final UserScrapRepository userScrapRepository;
    
    public Slice<ScrapKeywordNodeResult> getScrapKeywordNodes(Long userId, int page) {
        Pageable pageable = PageRequest.of(page, 20);
        List<UserScrap> scraps = userScrapRepository.findKeywordScrapsByUserId(userId, page, 20);
        List<ScrapKeywordNodeResult> content = scraps.stream()
                .map(ScrapKeywordNodeResult::from)
                .collect(Collectors.toList());
        return SliceUtil.checkLastPage(pageable, content);
    }

    public Slice<NodeSummaryResult> getScrapKeywordSummaries(Long userId, int page) {
        Pageable pageable = PageRequest.of(page, 4);
        List<UserScrap> scraps = userScrapRepository.findKeywordScrapsByUserId(userId, page, 4);
        List<NodeSummaryResult> content = scraps.stream()
                .map(scrap -> NodeSummaryResult.from(scrap.getRecommendKeyword()))
                .collect(Collectors.toList());
        return SliceUtil.checkLastPage(pageable, content);
    }
}