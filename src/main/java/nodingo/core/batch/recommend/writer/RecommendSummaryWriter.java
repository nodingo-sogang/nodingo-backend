package nodingo.core.batch.recommend.writer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nodingo.core.keyword.domain.RecommendKeyword;
import nodingo.core.keyword.repository.RecommendKeywordRepository;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class RecommendSummaryWriter {

    private final RecommendKeywordRepository recommendKeywordRepository;

    @Bean
    public ItemWriter<RecommendKeyword> recommendSummaryItemWriter() {
        return chunk -> {
            if (chunk.isEmpty()) return;
            recommendKeywordRepository.saveAll(chunk.getItems());
            log.info(">>>> [Recommend Summary Writer] Saved {} AI briefings.", chunk.getItems().size());
        };
    }
}