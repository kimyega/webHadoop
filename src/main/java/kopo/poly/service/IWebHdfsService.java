package kopo.poly.service;

import kopo.poly.dto.WebHdfsDTO;

public interface IWebHdfsService {

    String HDFS_URI = "http://192.168.48.129:9870/webhdfs/v1";
    String USER_NAME = "hadoop";

    String upload(WebHdfsDTO pDTO);

    String delete(WebHdfsDTO pDTO);

    String list(WebHdfsDTO pDTO);
}
