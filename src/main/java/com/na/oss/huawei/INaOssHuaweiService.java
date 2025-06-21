package com.na.oss.huawei;

import com.na.oss.config.NaAutoOssConfig;
import com.na.oss.dto.NaOssDto;

public interface INaOssHuaweiService {
    /**
     * 开发文档   https://support.huaweicloud.com/sdk-java-devg-obs/obs_21_0406.html
     */

    /**
     * 上传文件
     * @param dto
     * @param naAutoOssConfig
     * @return
     */
    NaOssDto upload(NaOssDto dto,
                    NaAutoOssConfig naAutoOssConfig);

    /**
     * 删除文件
     * @param dto
     * @param naAutoOssConfig
     * @return
     */
    NaOssDto delete(NaOssDto dto,
                    NaAutoOssConfig naAutoOssConfig);
}
