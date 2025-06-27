package com.na.oss.qiniu.impl;

import com.na.oss.config.NaAutoOssConfig;
import com.na.oss.dto.NaOssDto;
import com.na.oss.enums.NaOssFileOptStatus;
import com.na.oss.qiniu.INaOssQiNiuService;
import com.na.oss.utils.NaOssFileUtil;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class NaOssQiNiuServiceImpl implements INaOssQiNiuService {
    @Autowired
    private NaAutoOssConfig autoOssConfig;

    private BucketManager initBucketManager(NaAutoOssConfig autoOssConfig) {
        return new BucketManager(initAuth(autoOssConfig),new com.qiniu.storage.Configuration());
    }

    private Auth initAuth(NaAutoOssConfig autoOssConfig) {
        return Auth.create(autoOssConfig.getQiNiuAccessKey(),
                autoOssConfig.getQiNiuSecretKey());
    }

    private UploadManager initUploadManager(NaAutoOssConfig autoOssConfig){
        return new UploadManager(new com.qiniu.storage.Configuration());
    }

    private String getToken(NaAutoOssConfig autoOssConfig) {
        Auth auth = initAuth(autoOssConfig);
        return auth.uploadToken(autoOssConfig.getQiNiuBucketName());
    }

    @Override
    public NaOssDto upload(NaOssDto dto, NaAutoOssConfig naAutoOssConfig) throws IOException {
        dto.setFromType(NaOssDto.FromType.QINIU);
        naAutoOssConfig = (naAutoOssConfig != null) ? naAutoOssConfig : autoOssConfig;

        if(dto.isFileNull()){
            dto.setStatus(NaOssFileOptStatus.ERROR);
            return dto;
        }

        // 文件类型校验
        if (!NaOssFileUtil.checkFileType(dto, naAutoOssConfig)) {
            dto.setStatus(NaOssFileOptStatus.DATA_CHECK_ERROR);
            return dto;
        }

        // 文件大小校验
        if (!NaOssFileUtil.checkFileSize(dto, naAutoOssConfig)) {
            dto.setStatus(NaOssFileOptStatus.DATA_CHECK_ERROR);
            return dto;
        }

        // 构建目标文件名和路径
        dto = NaOssFileUtil.conversionToDto(dto);

        if(naAutoOssConfig.getQiNiuAutoDelete()){
            NaOssDto delDto = new NaOssDto(dto.getStorageFilePath() + dto.getNewFileName());
            // 注意删除时传递的为全路径， 例如  images/2024/04/13/1712995109396.jpg
            delete(delDto, naAutoOssConfig);
        }

        String token = getToken(naAutoOssConfig);

        UploadManager uploadManager = initUploadManager(naAutoOssConfig);
        Response response = uploadManager.put(dto.getUploadFile() != null ? dto.getUploadFile().getInputStream() : dto.getInputStream(),
                dto.getStorageFilePath() + dto.getNewFileName(),
                token,
                null,
                null);
        if (!response.isOK()) {
            dto.setStatus(NaOssFileOptStatus.ERROR);
            dto.setQiniuResponse(response);
            return dto;
        }

        dto.setStatus(NaOssFileOptStatus.DONE);
        dto.setDomain(autoOssConfig.getQiNiuDomain());
        dto.setBucket(autoOssConfig.getQiNiuBucketName());
        return dto;
    }

    @Override
    public NaOssDto delete(NaOssDto dto, NaAutoOssConfig naAutoOssConfig) throws QiniuException {
        dto.setFromType(NaOssDto.FromType.QINIU);
        naAutoOssConfig = (naAutoOssConfig != null) ? naAutoOssConfig : autoOssConfig;
        if(StringUtils.isEmpty(dto.getStorageFilePath())){
            dto.setStatus(NaOssFileOptStatus.DATA_CONFIG_PARAMS_ERROR);
            return dto;
        }
        BucketManager bucketManager = initBucketManager(naAutoOssConfig);

        bucketManager.delete(autoOssConfig.getQiNiuBucketName(), dto.getStorageFilePath());

        dto.setStatus(NaOssFileOptStatus.REMOVE);
        return dto;
    }
}
