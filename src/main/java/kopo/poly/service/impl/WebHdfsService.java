package kopo.poly.service.impl;

import kopo.poly.dto.WebHdfsDTO;
import kopo.poly.service.IWebHdfsService;
import kopo.poly.util.CmmUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * WebHDFS와 REST API 통신을 통해
 * 파일 업로드(WRITE), 삭제(DELETE), 조회(LISTSTATUS)를 수행하는 서비스 클래스
 * <p>
 * WebClient : Spring WebFlux 기반의 비동기(Non-blocking) HTTP 클라이언트
 */
@Slf4j
@Service
public class WebHdfsService implements IWebHdfsService {

    // WebClient 인스턴스 생성 (기본값: 비동기 HTTP 클라이언트)
    private final WebClient webClient = WebClient.create();

    /**
     * WebHDFS 업로드 (CREATE) 기능
     * 1단계: NameNode로 업로드 요청 (307 Redirect 응답 수신)
     * 2단계: Redirect된 DataNode URL로 실제 파일 데이터 PUT
     */
    @Override
    public String upload(WebHdfsDTO pDTO) {

        log.info("{}.upload Start!", this.getClass().getName());
        log.info("pDTO : {}", pDTO);

        String hdfsPath = CmmUtil.nvl(pDTO.getPath());
        String content = CmmUtil.nvl(pDTO.getContent());

        // CREATE 요청
        String firstUri = HDFS_URI + hdfsPath + "?op=CREATE&user.name=" + USER_NAME + "&overwrite=true";
        log.info("firstUri : {}", firstUri);

        // Step 1: NameNode로 업로드 요청 (Redirect 받을 것)
        return webClient.put()
                .uri(firstUri)
                .bodyValue(content)
                .exchangeToMono(response -> {

                    log.info("response status : {}", response.statusCode());

                    // Redirect (307/308)
                    if (response.statusCode().is3xxRedirection()) {
                        String redirectUri = response.headers().asHttpHeaders().getLocation().toString();
                        log.info("Redirect URI: {}", redirectUri);

                        // Step 2: DataNode로 실제 업로드
                        return webClient.put()
                                .uri(redirectUri)
                                .bodyValue(content)
                                .retrieve()
                                .bodyToMono(String.class);
                    } else {
                        return response.bodyToMono(String.class);
                    }
                })
                .block();
    }

    /**
     * WebHDFS 파일 삭제 기능
     * op=DELETE 요청으로 해당 경로 파일/디렉토리 제거
     */
    @Override
    public String delete(WebHdfsDTO pDTO) {

        log.info("{}.delete Start!", this.getClass().getName());
        log.info("pDTO : {}", pDTO);

        // 삭제할 경로
        String hdfsPath = CmmUtil.nvl(pDTO.getPath());

        // DELETE API 호출 URL 구성
        String uri = HDFS_URI + hdfsPath + "?op=DELETE" + "&user.name=" + USER_NAME;
        log.info("uri : {}", uri);


        // DELETE 요청 수행 (응답 JSON: {"boolean":true/false})
        return webClient.delete().uri(uri).retrieve().bodyToMono(String.class).block();
    }

    /**
     * WebHDFS 파일 목록 조회 기능
     * op=LISTSTATUS 요청으로 디렉터리 내 파일 리스트 반환
     */
    @Override
    public String list(WebHdfsDTO pDTO) {

        log.info("{}.listFiles Start!", this.getClass().getName());
        log.info("pDTO : {}", pDTO);

        // 조회할 경로
        String hdfsPath = CmmUtil.nvl(pDTO.getPath());

        // LISTSTATUS API 호출 URL 구성
        String uri = HDFS_URI + hdfsPath + "?op=LISTSTATUS" + "&user.name=" + USER_NAME;
        log.info("uri : {}", uri);


        // GET 요청 → 파일 목록(JSON 문자열 형태)
        return webClient.get().uri(uri).retrieve().bodyToMono(String.class).block();
    }
}