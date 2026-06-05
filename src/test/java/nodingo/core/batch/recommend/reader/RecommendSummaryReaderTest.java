package nodingo.core.batch.recommend.reader;

import nodingo.core.keyword.domain.RecommendKeyword;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManagerFactory;
import org.mockito.Mock;

@ExtendWith(MockitoExtension.class)
class RecommendSummaryReaderTest {

    @Mock
    private EntityManagerFactory entityManagerFactory;

    @InjectMocks
    private RecommendSummaryReader recommendSummaryReader;

    @Test
    @DisplayName("RecommendSummary JpaPagingItemReader가 정상적인 쿼리와 파라미터 설정으로 생성되어야 한다")
    void recommendSummaryItemReaderCreationTest() {
        // given
        LocalDateTime dummyRequestTime = LocalDateTime.of(2026, 5, 16, 10, 0, 0);

        // when
        JpaPagingItemReader<RecommendKeyword> reader = recommendSummaryReader.recommendSummaryItemReader();

        // then
        assertThat(reader).isNotNull();
        String queryString = (String) ReflectionTestUtils.getField(reader, "queryString");
        Integer pageSize = (Integer) ReflectionTestUtils.getField(reader, "pageSize");

        @SuppressWarnings("unchecked")
        Map<String, Object> parameterValues = (Map<String, Object>) ReflectionTestUtils.getField(reader, "parameterValues");
        assertThat(queryString).isEqualTo("SELECT r FROM RecommendKeyword r WHERE r.targetDate = :targetDate");
        assertThat(pageSize).isEqualTo(100);

        assertThat(parameterValues).containsEntry("targetDate", dummyRequestTime.toLocalDate());
    }
}