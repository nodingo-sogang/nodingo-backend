package nodingo.core.game.utils;

import org.springframework.stereotype.Component;

@Component
public class DefaultGamePolicy implements GamePolicy {
    @Override
    public int getExploreXp() { return 5; }

    @Override
    public int getFirstVisitXp() { return 10; }

    @Override
    public int getScrapXp() { return 3; }
}