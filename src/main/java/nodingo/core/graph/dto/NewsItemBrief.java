package nodingo.core.graph.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsItemBrief {
    private Long id;
    private String title;
    private String url;
    private String outlet;

    @JsonFormat(pattern = "yyyy.MM.dd")
    private LocalDate date;

    private String snippet;
}