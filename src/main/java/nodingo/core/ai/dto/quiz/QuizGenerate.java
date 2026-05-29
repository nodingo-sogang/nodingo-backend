package nodingo.core.ai.dto.quiz;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

public class QuizGenerate {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        @JsonProperty("keyword_id")
        private Long keywordId;

        @JsonProperty("word")
        private String word;

        @JsonProperty("summary")
        private String summary;

        @JsonProperty("related_news")
        private List<RelatedNews> relatedNews;

        @JsonProperty("num_questions")
        private int numQuestions;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RelatedNews {
        @JsonProperty("news_id")
        private Long newsId;

        @JsonProperty("title")
        private String title;

        @JsonProperty("body")
        private String body;

        @JsonProperty("url")
        private String url;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        @JsonProperty("keyword_id")
        private Long keywordId;

        @JsonProperty("quizzes")
        private List<QuizInfo> quizzes;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuizInfo {
        @JsonProperty("question")
        private String question;

        @JsonProperty("options")
        private List<String> options;

        @JsonProperty("answer_index")
        private Integer answerIndex;

        @JsonProperty("explanation")
        private String explanation;

        @JsonProperty("source_news_ids")
        private List<Long> sourceNewsIds;
    }
}