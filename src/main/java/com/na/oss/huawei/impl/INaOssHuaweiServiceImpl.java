package com.na.oss.huawei.impl;

import com.na.oss.config.NaAutoOssConfig;
import com.na.oss.dto.NaOssDto;
import com.na.oss.enums.NaOssFileOptStatus;
import com.na.oss.huawei.INaOssHuaweiService;
import com.na.oss.utils.NaOssFileUtil;
import com.obs.services.ObsClient;
import com.obs.services.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@Service
public class INaOssHuaweiServiceImpl implements INaOssHuaweiService {
    @Autowired
    private NaAutoOssConfig autoOssConfig;

    private ObsClient init(NaAutoOssConfig autoOssConfig) {
        ObsClient obsClient = new ObsClient(autoOssConfig.getHuaweiAccessKey(),
                autoOssConfig.getHuaweiSecretKey(),
                autoOssConfig.getHuaweiEndpoint());
        boolean exists = obsClient.headBucket(autoOssConfig.getHuaweiBucketName());
        if(!exists){
            ObsBucket obsBucket = new ObsBucket();
            obsBucket.setBucketName(autoOssConfig.getHuaweiBucketName());
            obsBucket.setAcl(AccessControlList.REST_CANNED_PUBLIC_READ);
            obsBucket.setLocation(autoOssConfig.getHuaweiArea());
            obsClient.createBucket(obsBucket);
        }
        return obsClient;
    }

    @Override
    public NaOssDto upload(NaOssDto dto, NaAutoOssConfig naAutoOssConfig) throws IOException {
        dto.setFromType(NaOssDto.FromType.HUAWEI);
        naAutoOssConfig = (naAutoOssConfig != null) ? naAutoOssConfig : autoOssConfig;

        if(dto.isFileNull()){
            dto.setStatus(NaOssFileOptStatus.ERROR);
            return dto;
        }
        ObsClient obsClient = init(naAutoOssConfig);

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

        String filePath = dto.getStorageFilePath() + dto.getNewFileName();

        /**
         * 上传到华为云
         */
        ObjectMetadata objectMetadata = new ObjectMetadata();
        Long available = dto.getUploadFile() != null ? new ByteArrayInputStream(dto.getUploadFile().getBytes()).available() :
                NaOssFileUtil.getInputStreamSize(dto.getInputStream());
        objectMetadata.setContentLength(available);
        objectMetadata.setCacheControl("no-cache");
        objectMetadata.setContentType(NaOssFileUtil.getContentType(dto.getFileName().substring(dto.getFileName().lastIndexOf("."))));
        objectMetadata.setContentDisposition("inline; filename=\"" + dto.getFileName() + "\"");

        PutObjectRequest putObjectRequest = new PutObjectRequest();
        putObjectRequest.setBucketName(naAutoOssConfig.getHuaweiBucketName());
        putObjectRequest.setInput(dto.getUploadFile() != null ? dto.getUploadFile().getInputStream() : dto.getInputStream());
        putObjectRequest.setObjectKey(filePath);
        putObjectRequest.setAcl(AccessControlList.REST_CANNED_PUBLIC_READ);
        putObjectRequest.setMetadata(objectMetadata);
        obsClient.putObject(putObjectRequest);

        dto.setStatus(NaOssFileOptStatus.DONE);
        dto.setDomain(naAutoOssConfig.getHuaweiDomain());
        dto.setBucket(naAutoOssConfig.getHuaweiBucketName());

        return dto;
    }

    @Override
    public NaOssDto delete(NaOssDto dto, NaAutoOssConfig naAutoOssConfig) {
        dto.setFromType(NaOssDto.FromType.HUAWEI);
        naAutoOssConfig = (naAutoOssConfig != null) ? naAutoOssConfig : autoOssConfig;
        if(dto.isPathNull()){
            dto.setStatus(NaOssFileOptStatus.ERROR);
            return dto;
        }


        ObsClient obsClient = init(naAutoOssConfig);

        DeleteObjectsRequest deleteRequest = new DeleteObjectsRequest();
        deleteRequest.setBucketName(naAutoOssConfig.getHuaweiBucketName());
        deleteRequest.addKeyAndVersion(dto.getStorageFilePath());
        obsClient.deleteObjects(deleteRequest);

        dto.setStatus(NaOssFileOptStatus.REMOVE);
        return dto;
    }
}
