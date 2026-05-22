package nodingo.core.keyword.service.query;

import lombok.RequiredArgsConstructor;
import nodingo.core.keyword.domain.Keyword;
import nodingo.core.keyword.repository.KeywordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class KeywordQueryService {
    private final KeywordRepository keywordRepository;

    public Map<String, Keyword> getExistingKeywordsMap(Collection<String> normalizedWords) {
        if (normalizedWords == null || normalizedWords.isEmpty()) {
            return new HashMap<>();
        }
        return keywordRepository.findByNormalizedWordIn(normalizedWords).stream()
                .collect(Collectors.toMap(
                        Keyword::getNormalizedWord,
                        Function.identity(),
                        (existing, replacement) -> existing
                ));
    }
}
