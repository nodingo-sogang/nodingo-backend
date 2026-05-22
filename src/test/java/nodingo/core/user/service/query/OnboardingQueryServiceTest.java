package nodingo.core.user.service.query;

import nodingo.core.keyword.domain.Keyword;
import nodingo.core.keyword.repository.KeywordRepository;
import nodingo.core.user.domain.InterestLevel;
import nodingo.core.user.domain.UserPersona;
import nodingo.core.user.dto.result.KeywordListResult;
import nodingo.core.user.dto.result.PersonaListResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
@ExtendWith(MockitoExtension.class)
class OnboardingQueryServiceTest {

    @InjectMocks
    private OnboardingQueryService onboardingQueryService;

    @Mock
    private KeywordRepository keywordRepository;

    private final Long testUserId = 1L;

    @Test
    @DisplayName("1. 대분류(Persona) 목록 조회: 모든 Enum 값이 유저 ID와 함께 정상 반환되어야 함")
    void getPersonasTest() {
        // when
        PersonaListResult result = onboardingQueryService.getPersonas();

        // then
        assertThat(result.getContents()).hasSize(UserPersona.values().length);
        assertThat(result.getContents().get(0).getName()).isEqualTo(UserPersona.POLITICS.name());
        assertThat(result.getContents().get(0).getDescription()).isEqualTo(UserPersona.POLITICS.getDescription());
    }

    @Test
    @DisplayName("2. 중분류(Macro) 목록 조회: 페르소나와 MACRO 레벨 조건으로 필터링되어야 함")
    void getMacroKeywordsTest() {
        // given
        UserPersona persona = UserPersona.TECHNOLOGY;
        Long macroId = 10L;
        Keyword macroKeyword = createMockKeyword(macroId, "인공지능", persona, InterestLevel.MACRO, null);
        Pageable limit = PageRequest.of(0, 6);

        given(keywordRepository.findAllByPersonaAndLevel(persona, InterestLevel.MACRO, limit))
                .willReturn(List.of(macroKeyword));

        // when
        KeywordListResult result = onboardingQueryService.getMacroKeywords(persona);

        // then
        assertThat(result.getContents()).hasSize(1);
        assertThat(result.getContents().get(0).getId()).isEqualTo(macroId);
        assertThat(result.getContents().get(0).getWord()).isEqualTo("인공지능");
        verify(keywordRepository, times(1)).findAllByPersonaAndLevel(persona, InterestLevel.MACRO, limit);
    }

    @Test
    @DisplayName("3. 소분류(Specific) 목록 조회: 부모 ID와 SPECIFIC 레벨 조건으로 필터링되어야 함")
    void getSpecificKeywordsTest() {
        // given
        Long parentId = 10L;
        Long specificId = 101L;
        Keyword specificKeyword = createMockKeyword(specificId, "LLM", UserPersona.TECHNOLOGY, InterestLevel.SPECIFIC, null);
        Pageable limit = PageRequest.of(0, 6);

        given(keywordRepository.findAllByParentIdAndLevel(parentId, InterestLevel.SPECIFIC, limit))
                .willReturn(List.of(specificKeyword));

        // when
        KeywordListResult result = onboardingQueryService.getSpecificKeywords(parentId);

        // then
        assertThat(result.getContents()).hasSize(1);
        assertThat(result.getContents().get(0).getId()).isEqualTo(specificId);
        assertThat(result.getContents().get(0).getWord()).isEqualTo("LLM");
        verify(keywordRepository, times(1)).findAllByParentIdAndLevel(parentId, InterestLevel.SPECIFIC, limit);
    }

    /**
     * ID 주입 헬퍼 메서드
     */
    private Keyword createMockKeyword(Long id, String word, UserPersona persona, InterestLevel level, Keyword parent) {
        Keyword keyword = Keyword.createOnboardingKeyword(word, persona, level, parent);
        ReflectionTestUtils.setField(keyword, "id", id);
        return keyword;
    }
}