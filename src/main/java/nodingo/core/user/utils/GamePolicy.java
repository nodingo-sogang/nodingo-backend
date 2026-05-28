package nodingo.core.user.utils;

public interface GamePolicy {
    int getExploreXp();      // 노드 탐험 시 XP
    int getFirstVisitXp();   // 첫 방문 시 XP
    int getScrapXp();        // 스크랩 시 XP
}