package nodingo.core.batch.service.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nodingo.core.ai.dto.newsBatch.NewsBatch;
import nodingo.core.keyword.repository.KeywordRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeywordCacheService {
    private final KeywordRepository keywordRepository;

    @Cacheable(value = "batch:keyword", key = "'all'")
    public List<NewsBatch.ExistingKeywordInput> getAllKeywords() {
        List<NewsBatch.ExistingKeywordInput> keywords = keywordRepository.findAll().stream()
                .map(k -> NewsBatch.ExistingKeywordInput.builder()
                        .keywordId(k.getId())
                        .word(k.getWord())
                        .normalizedWord(k.getNormalizedWord())
                        .embedding(k.getEmbedding())
                        .build())
                .toList();

        log.info(">>>> [Keyword Cache] Loaded {} keywords from DB.", keywords.size());
        return keywords;
    }
}
