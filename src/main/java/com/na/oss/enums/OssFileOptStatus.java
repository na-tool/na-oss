package com.na.oss.enums;

import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

/**
 * OSS 文件操作状态枚举。
 * <p>
 * 上传、查询、删除等操作的结果状态统一使用该枚举表示，
 * 并通过 {@link LocaleContextHolder} 自动选择中英文提示。
 * </p>
 */
public enum OssFileOptStatus implements IOssFileOptStatusService {

    /** 上传中 */
    UPLOAD_ING("upload_ing", "上传中", "上传中", "oss.file.upload_ing"),

    /** 上传完成 */
    DONE("done", "done", "上传完成", "oss.file.done"),

    /** 操作失败 */
    ERROR("error", "error", "失败", "oss.file.error"),

    /** 删除成功 */
    REMOVE("remove", "remove", "删除", "oss.file.remove"),

    /** 查询成功 */
    QUERY_SUCCESS("query_success", "query_success", "查询成功", "oss.file.query.success"),

    /** 配置参数错误 */
    DATA_CONFIG_PARAMS_ERROR("data_config_params_error", "data_config_params_error", "配置参数错误", "oss.file.data.config.error"),

    /** 数据校验错误（文件为空、类型或大小不符合要求等） */
    DATA_CHECK_ERROR("data_check_error", "data_check_error", "数据校验错误", "oss.file.data.check.error"),
    ;

    /** 状态编码 */
    private final String code;

    /** 英文提示 */
    private final String enMsg;

    /** 中文提示 */
    private final String zhMsg;

    /** 国际化消息 key */
    private final String msgKey;

    OssFileOptStatus(String code, String enMsg, String zhMsg, String msgKey) {
        this.code = code;
        this.enMsg = enMsg;
        this.zhMsg = zhMsg;
        this.msgKey = msgKey;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String enMsg() {
        return enMsg;
    }

    @Override
    public String zhMsg() {
        return zhMsg;
    }

    /**
     * 根据当前请求语言环境返回中文或英文提示。
     *
     * @return 本地化提示信息
     */
    @Override
    public String getMsg() {
        if (Locale.SIMPLIFIED_CHINESE.getLanguage().equals(LocaleContextHolder.getLocale().getLanguage())) {
            return this.zhMsg;
        }
        return this.enMsg;
    }

    @Override
    public String msgKey() {
        return msgKey;
    }

    /**
     * 获取国际化提示。
     * <p>
     * 项目未引入 MessageSource 时退化为 {@link #getMsg()}；
     * 需要从资源文件读取时，可在外层结合 {@link #msgKey()} 解析。
     * </p>
     *
     * @return 本地化提示信息
     */
    @Override
    public String getLanguageMsg() {
        return getMsg();
    }
}
