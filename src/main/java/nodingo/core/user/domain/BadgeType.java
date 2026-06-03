package nodingo.core.user.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BadgeType {
    FIRST_VISIT("first_visit", "첫 발걸음", "attendance", "첫 출석을 기록했어요", "첫 출석"),
    FIRST_EXPLORE("first_explore", "첫 탐험", "explore", "첫 노드를 탐험했어요", "노드 1개 탐험"),
    EXPLORE_10("explore_10", "동네 탐험가", "explore", "노드 10개를 발견했어요", "노드 10개 탐색"),
    EXPLORE_50("explore_50", "지도 완성자", "explore", "노드 50개를 정복했어요", "노드 50개 탐색"),
    FIRST_QUIZ("first_quiz", "첫 도전", "quiz", "퀴즈를 처음으로 완료했어요", "퀴즈 첫 완료"),
    QUIZ_10("quiz_10", "퀴즈 고수", "quiz", "퀴즈를 10회 완료했어요", "퀴즈 10회 완료"),
    FIRST_SCRAP("first_scrap", "첫 스크랩", "scrap", "키워드를 처음 스크랩했어요", "키워드 첫 스크랩"),
    ATTENDANCE_7("attendance_7", "일주일 개근", "attendance", "7일 연속 출석 달성!", "7일 연속 출석"),
    ATTENDANCE_30("attendance_30", "한 달 개근", "attendance", "30일 연속 출석 달성!", "30일 연속 출석");

    private final String id;
    private final String name;
    private final String category;
    private final String description;
    private final String condition;
}