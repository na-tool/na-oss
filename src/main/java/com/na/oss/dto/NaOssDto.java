package com.na.oss.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.na.oss.enums.NaOssFileOptStatus;
import com.qiniu.http.Response;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NaOssDto {
    /**
     * 文件唯一标识
     */
    private String id;

    /**
     * 路径前缀
     */
    private String pathPrefix;

    /**
     * 上传的文件  【必填】
     */
    @JsonIgnore
    private MultipartFile uploadFile;
    @JsonIgnore
    private InputStream inputStream;

    /**
     * 原始文件名
     */
    private String sourceFileName;

    /**
     * 新文件名
     * 例如    1719471001213.JPG
     */
    private String newFileName;

    /**
     * 存储路径,
     * 上传时响应的路径  例如  img/2024/06/27/
     * 注意删除时传递的为全路径， 例如  images/2024/04/13/1712995109396.jpg
     */
    private String storageFilePath;

    /**
     * bucket
     */
    private String bucket;

    /**
     * 状态有：uploading done error removed
     */
    private NaOssFileOptStatus status;

    /**
     * 自定义域名
     */
    private String domain;

    /**
     * 上传来源， 七牛云：QINIU   阿里: ALI  腾讯：TENCENT  华为: HUAWEI  本地：LOCAL
     * 根据来源  选填
     */
    private String fromType = FromType.QINIU;

    private Response qiniuResponse;

    public class FromType {
        public final static String QINIU = "QINIU";
        public final static String ALI = "ALI";
        public final static String TENCENT = "TENCEN";
        public final static String HUAWEI = "HUAWEI";
        public final static String LOCAL = "LOCAL";
    }

    public String getSourceFileName() {
        return uploadFile == null ? sourceFileName : uploadFile.getOriginalFilename();
    }

    @JsonIgnore
    public Boolean isFileNull(){
        return uploadFile == null && inputStream == null;
    }

    @JsonIgnore
    public String getFileName() {
        return StringUtils.isBlank(newFileName) ? getSourceFileName() : newFileName;
    }

    @JsonIgnore
    public Boolean isPathNull(){
        return storageFilePath == null;
    }

    public NaOssDto(String storageFilePath) {
        this.storageFilePath = storageFilePath;
    }
}
