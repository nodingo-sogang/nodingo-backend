package nodingo.core.batch.news.writer;

import nodingo.core.ai.client.AiClient;
import nodingo.core.ai.dto.newsBatch.NewsBatch;
import nodingo.core.batch.service.cache.KeywordCacheService;
import nodingo.core.global.util.NewsSummarizer;
import nodingo.core.keyword.repository.KeywordRelationRepository;
import nodingo.core.keyword.repository.KeywordRepository;
import nodingo.core.news.domain.News;
import nodingo.core.news.repository.NewsRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.Chunk;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.atMostOnce;


@ExtendWith(MockitoExtension.class)
class NewsAiWriterTest {

    @Mock private NewsRepository newsRepository;
    @Mock private KeywordRepository keywordRepository;
    @Mock private KeywordRelationRepository keywordRelationRepository;
    @Mock private AiClient aiClient;
    @Mock private NewsSummarizer newsSummarizer;
    @Mock private KeywordCacheService keywordCacheService;

    @InjectMocks private NewsAiWriter writer;

    @Test
    @DisplayName("AI 분석 결과를 바탕으로 임베딩, 요약, 키워드 및 키워드 관계를 저장한다")
    void write_Success() throws Exception {
        // given
        News news = News.create("uri1", "title1", "original body", "url", "kor", 0.0, LocalDateTime.now());
        ReflectionTestUtils.setField(news, "id", 1L);

        given(newsRepository.saveAll(anyList())).willAnswer(i -> i.getArgument(0));

        given(keywordCacheService.getAllKeywords()).willReturn(Collections.emptyList());

        NewsBatch.Response aiResponse = NewsBatch.Response.builder()
                .newsResults(List.of(NewsBatch.NewsAnalysisResult.builder()
                        .newsId(1L)
                        .embedding(new float[]{0.1f})
                        .keywords(Collections.emptyList())
                        .build()))
                .keywordRelations(List.of(new NewsBatch.KeywordRelationResult(10L, 11L, "키워드C", "키워드D", 0.95)))
                .build();

        given(aiClient.analyzeNewsBatch(any(NewsBatch.Request.class))).willReturn(aiResponse);
        given(newsSummarizer.summarize(any(News.class))).willReturn("요약본");

        Chunk<News> chunk = new Chunk<>(List.of(news));

        // when
        writer.write(chunk);

        // then
        assertThat(news.getBody()).isEqualTo("요약본");

        verify(keywordCacheService).getAllKeywords();
        verify(newsRepository, times(2)).saveAll(anyList());
        verify(aiClient).analyzeNewsBatch(any(NewsBatch.Request.class));
        verify(keywordRelationRepository, atMostOnce()).saveAll(anyList());
    }
}