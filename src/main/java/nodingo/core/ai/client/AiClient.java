package nodingo.core.ai.client;

import nodingo.core.ai.dto.graphPreview.GraphPreview;
import nodingo.core.ai.dto.keyword.KeywordRecommend;
import nodingo.core.ai.dto.keyword.KeywordSummary;
import nodingo.core.ai.dto.newsBatch.NewsBatch;
import nodingo.core.ai.dto.relation.NewsRelationAnalysis;
import nodingo.core.ai.dto.userEmbedding.UserEmbedding;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "aiClient", url = "${ai.server.url}")
public interface AiClient {
    @PostMapping("/v1/news/analyze-batch")
    NewsBatch.Response analyzeNewsBatch(@RequestBody NewsBatch.Request request);

    @PostMapping("/v1/news/build-news-relations")
    NewsRelationAnalysis.Response buildNewsRelations(@RequestBody NewsRelationAnalysis.Request request);

    @PostMapping("/v1/users/init-embedding")
    UserEmbedding.Response initUserEmbedding(@RequestBody UserEmbedding.InitRequest request);

    @PostMapping("/v1/users/update-embedding")
    UserEmbedding.Response updateUserEmbedding(@RequestBody UserEmbedding.UpdateRequest request);

    @PostMapping("/v1/recommend-keywords")
    KeywordRecommend.Response recommendKeywords(@RequestBody KeywordRecommend.Request request);

    @PostMapping("/v1/recommend-keywords/summarize")
    KeywordSummary.Response summarizeKeywords(@RequestBody KeywordSummary.Request request);

    @PostMapping("/v1/graph/preview")
    GraphPreview.Response getGraphPreview(@RequestBody GraphPreview.Request request);

    @GetMapping("/health")
    Map<String, Object> healthCheck();
}
