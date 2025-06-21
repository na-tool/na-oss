package com.na.oss.ali;

import com.aliyun.oss.model.OSSObjectSummary;
import com.na.oss.config.NaAutoOssConfig;
import com.na.oss.dto.NaOssDto;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public interface INaOssAliService {
    /**
     *  上传文件到阿里云OSS
     * @param dto
     * @param autoOssConfig
     * @return
     */
    NaOssDto upload(NaOssDto dto,
                    NaAutoOssConfig autoOssConfig);

    /**
     * 根据根据文件路径和文件名查询文件 images/2024/02/02/1706867083325.png
     * @param dto
     * @param autoOssConfig
     * @return
     */
    NaOssDto queryByFileName(NaOssDto dto,
                             NaAutoOssConfig autoOssConfig);

    /**
     * 查询所有的文件
     * @return
     */
    List<OSSObjectSummary> queryAll(Integer maxKeys,
                                    NaAutoOssConfig autoOssConfig);

    /**
     * 根据根据文件路径和文件名下载文件
     * fileName 需要传全路径  images/2024/02/02/1706867083325.png
     * @param dto
     * @param response
     */
    void downloadByFileName(NaOssDto dto,
                            HttpServletResponse response,
                            NaAutoOssConfig autoOssConfig) throws IOException;

    /**
     * 根据文件路径和文件名删除文件
     * fileName 需要传全路径  images/2024/02/02/1706867083325.png
     * @param dto
     * @return
     */
    NaOssDto deleteByFileName(NaOssDto dto,
                              NaAutoOssConfig autoOssConfig);
}
