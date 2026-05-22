package nodingo.core.user.service.command;

import nodingo.core.keyword.domain.Keyword;
import nodingo.core.keyword.repository.KeywordRepository;
import nodingo.core.user.domain.InterestLevel;
import nodingo.core.user.domain.User;
import nodingo.core.user.domain.UserInterest;
import nodingo.core.user.domain.UserPersona;
import nodingo.core.user.dto.command.InterestCommand;
import nodingo.core.user.dto.command.SaveOnboardingCommand;
import nodingo.core.user.repository.UserInterestRepository;
import nodingo.core.user.repository.UserRepository;
import nodingo.core.user.service.vector.UserVectorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OnboardingServiceTest {

    @InjectMocks
    private OnboardingService onboardingService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private KeywordRepository keywordRepository;

    @Mock
    private UserInterestRepository userInterestRepository;

    @Mock
    private UserVectorService userVectorService;

    @Test
    @DisplayName("온보딩 정보 저장 성공 - 관심사 저장 및 유저 벡터 초기화 확인")
    void saveOnboardingInfo_Success() {
        // 1. Given
        Long userId = 1L;
        User user = User.create("naver", "sub-123", "yena", "최예나", "yena@yena.com");

        ReflectionTestUtils.setField(user, "id", userId);

        Long macroId = 10L;
        List<Long> specificIds = List.of(101L, 102L);

        SaveOnboardingCommand command = SaveOnboardingCommand.builder()
                .userId(userId)
                .personas(List.of(UserPersona.TECHNOLOGY))
                .interest(InterestCommand.builder()
                        .macroId(macroId)
                        .specificIds(specificIds)
                        .build())
                .build();

        Keyword macroK = Keyword.create("기술");
        Keyword specK1 = Keyword.create("백엔드");
        Keyword specK2 = Keyword.create("AI");

        ReflectionTestUtils.setField(macroK, "id", macroId);
        ReflectionTestUtils.setField(specK1, "id", 101L);
        ReflectionTestUtils.setField(specK2, "id", 102L);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(keywordRepository.findAllById(anyList())).thenReturn(List.of(macroK, specK1, specK2));

        // 2. When
        onboardingService.saveOnboardingInfo(command);

        // 3. Then
        verify(userInterestRepository, times(1)).deleteTodayInterests(eq(userId), any(LocalDate.class));

        verify(userVectorService, times(1)).initUserEmbedding(eq(user), anyList());

        assertThat(user.getInterests()).hasSize(3);

        UserInterest macroInterest = user.getInterests().stream()
                .filter(ui -> ui.getLevel() == InterestLevel.MACRO)
                .findFirst().orElseThrow();

        assertThat(macroInterest.getKeyword().getWord()).isEqualTo("기술");
    }

    @Test
    @DisplayName("페르소나를 2개 이상 선택할 경우 예외가 발생한다")
    void saveOnboardingInfo_TooManyPersonas() {
        // Given
        Long userId = 1L;
        User user = User.create("naver", "sub-123", "doyoung", "김도영", "doyoung@yena.com");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        SaveOnboardingCommand command = SaveOnboardingCommand.builder()
                .userId(userId)
                .personas(List.of(UserPersona.TECHNOLOGY, UserPersona.ECONOMY))
                .interest(InterestCommand.builder().macroId(10L).specificIds(List.of()).build())
                .build();


        assertThrows(IllegalArgumentException.class, () -> {
            onboardingService.saveOnboardingInfo(command);
        });
    }
}