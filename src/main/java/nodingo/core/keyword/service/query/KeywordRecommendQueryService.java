package nodingo.core.keyword.service.query;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nodingo.core.ai.dto.keyword.KeywordRecommend;
import nodingo.core.keyword.dto.query.KeywordCandidate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

import nodingo.core.keyword.repository.KeywordRepository;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KeywordRecommendQueryService {
    private final KeywordRepository keywordRepository;

    public List<KeywordRecommend.CandidateKeyword> getDailyCandidateKeywords(LocalDate targetDate) {
        LocalDateTime startTime = targetDate.minusDays(1).atTime(LocalTime.of(5, 1));
        LocalDateTime endTime = targetDate.atTime(LocalTime.of(4, 59));

        List<KeywordCandidate> candidateKeywords = keywordRepository.findCandidateKeywords(startTime, endTime);

        if (candidateKeywords.isEmpty()) {
            log.warn(">>>> [Recommend] {} ~ {} 사이에 수집된 후보 키워드가 없습니다.", startTime, endTime);
        } else {
            log.info(">>>> [Recommend] {}일자 후보 키워드 {}개 로드 완료", targetDate, candidateKeywords.size());
        }

        return candidateKeywords.stream()
                .map(dto -> KeywordRecommend.CandidateKeyword.builder()
                        .keywordId(dto.getKeywordId())
                        .word(dto.getWord())
                        .normalizedWord(dto.getWord())
                        .embedding(dto.getEmbedding())
                        .recentImportance(1.0)
                        .isUserInterest(false)
                        .build())
                .collect(Collectors.toList());
    }
}
