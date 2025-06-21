package com.na.oss.qiniu;

import com.na.oss.config.NaAutoOssConfig;
import com.na.oss.dto.NaOssDto;

public interface INaOssQiNiuService {
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
