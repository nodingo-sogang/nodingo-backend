package nodingo.core.batch.recommend.writer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nodingo.core.keyword.domain.RecommendKeyword;
import nodingo.core.keyword.repository.RecommendKeywordRepository;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendWriter {

    private final RecommendKeywordRepository recommendKeywordRepository;

    @Bean
    public ItemWriter<List<RecommendKeyword>> recommendItemWriter() {
        return chunk -> {
            List<RecommendKeyword> allRecommendations = chunk.getItems().stream()
                    .flatMap(List::stream)
                    .collect(Collectors.toList());

            if (!allRecommendations.isEmpty()) {
                recommendKeywordRepository.saveAll(allRecommendations);
            }

            log.info(">>>> [Recommend Writer] Processed {} users / Saved {} keywords to DB.",
                    chunk.getItems().size(), allRecommendations.size());
        };
    }
}