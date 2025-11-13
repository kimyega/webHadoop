package kopo.poly.dto;

import lombok.*;

@Data
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WebHdfsDTO {

    private String path;
    private String content;
}
