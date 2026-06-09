package nodingo.core.user.service.vector;

import nodingo.core.ai.client.AiClient;
import nodingo.core.ai.dto.userEmbedding.UserEmbedding;
import nodingo.core.global.exception.ai.AiIntegrationException;
import nodingo.core.keyword.domain.Keyword;
import nodingo.core.keyword.repository.KeywordRepository;
import nodingo.core.news.domain.News;
import nodingo.core.news.repository.NewsRepository;
import nodingo.core.user.domain.User;
import nodingo.core.user.domain.UserScrap;
import nodingo.core.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;


import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class UserVectorServiceTest {

    @InjectMocks
    private UserVectorService userVectorService;

    @Mock private AiClient aiClient;
    @Mock private UserRepository userRepository;
    @Mock private NewsRepository newsRepository;
    @Mock private KeywordRepository keywordRepository;

    private final Long userId = 1L;
    private final float[] oldEmbedding = {0.1f, 0.1f, 0.1f};
    private final float[] newEmbedding = {0.9f, 0.9f, 0.9f};

    @Test
    @DisplayName("성공: 초기 온보딩 키워드로 AI 서버에서 임베딩을 받아 유저에게 설정한다")
    void initUserEmbedding_Success() {
        // given
        User user = spy(createMockUser());
        List<Keyword> selectedKeywords = List.of(
                createMockKeyword(1L, "정치", new float[]{0.1f, 0.1f}),
                createMockKeyword(2L, "경제", new float[]{0.2f, 0.2f})
        );

        UserEmbedding.Response mockResponse = UserEmbedding.Response.builder()
                .userId(userId)
                .embedding(newEmbedding)
                .build();

        given(aiClient.initUserEmbedding(any())).willReturn(mockResponse);

        // when
        userVectorService.initUserEmbedding(user, selectedKeywords);

        // then
        verify(user).updateEmbedding(newEmbedding);
        verify(aiClient).initUserEmbedding(any());
    }

    @Test
    @DisplayName("성공: 뉴스 스크랩 정보를 바탕으로 유저 임베딩을 비동기 업데이트한다")
    void updateUserEmbeddingAsync_Success() {
        // given
        Long newsId = 100L;
        User user = spy(createMockUser());
        News news = mock(News.class);
        UserScrap scrap = mock(UserScrap.class);

        given(newsRepository.findScrapDetail(userId, newsId)).willReturn(Optional.of(scrap));
        given(scrap.getUser()).willReturn(user);
        given(scrap.getNews()).willReturn(news);
        given(news.getEmbedding()).willReturn(new float[]{0.5f, 0.5f});

        UserEmbedding.Response mockRes = UserEmbedding.Response.builder()
                .userId(userId)
                .embedding(newEmbedding)
                .build();

        given(aiClient.updateUserEmbedding(any())).willReturn(mockRes);

        // when
        userVectorService.updateUserEmbeddingAsync(userId, newsId);

        // then
        verify(user).updateEmbedding(newEmbedding);
        verify(userRepository).saveAndFlush(user);
    }

    @Test
    @DisplayName("성공: 키워드 요약 스크랩 정보를 바탕으로 유저 임베딩을 비동기 업데이트한다")
    void updateKeywordEmbeddingAsync_Success() {
        // given
        Long keywordId = 50L;
        User user = spy(createMockUser());
        Keyword keyword = createMockKeyword(keywordId, "테스트키워드", new float[]{0.8f, 0.8f});

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(keywordRepository.findById(keywordId)).willReturn(Optional.of(keyword));

        UserEmbedding.Response mockRes = UserEmbedding.Response.builder()
                .userId(userId)
                .embedding(newEmbedding)
                .build();

        given(aiClient.updateUserEmbedding(any())).willReturn(mockRes);

        // when
        userVectorService.updateKeywordEmbeddingAsync(userId, keywordId);

        // then
        verify(user).updateEmbedding(newEmbedding);
        verify(userRepository).saveAndFlush(user);
        verify(aiClient).updateUserEmbedding(argThat(req ->
                req.getActivities().get(0).getType().equals("CLICK") // AI 서버 전달 타입 검증
        ));
    }

    @Test
    @DisplayName("실패: 초기 임베딩 생성 시 AI 서버 에러가 발생하면 AiIntegrationException을 던진다")
    void initUserEmbedding_Fail() {
        // given
        User user = createMockUser();
        given(aiClient.initUserEmbedding(any())).willThrow(new RuntimeException("AI Server Error"));

        // when & then
        assertThrows(AiIntegrationException.class, () ->
                userVectorService.initUserEmbedding(user, List.of(mock(Keyword.class)))
        );
    }

    // --- Helper Methods ---
    private User createMockUser() {
        User user = User.create(
                "google",
                "sub-123",
                "sungmin",
                "성민",
                "test@test.com",
                "성민닉네임",
                "https://nodingo.com/profile.png"
        );
        user.updateEmbedding(oldEmbedding);
        return user;
    }

    private Keyword createMockKeyword(Long id, String word, float[] embedding) {
        Keyword k = mock(Keyword.class);
        lenient().when(k.getId()).thenReturn(id);
        lenient().when(k.getWord()).thenReturn(word);
        lenient().when(k.getEmbedding()).thenReturn(embedding);
        return k;
    }
}