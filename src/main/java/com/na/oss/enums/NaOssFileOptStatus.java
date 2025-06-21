package com.na.oss.enums;

import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

public enum NaOssFileOptStatus implements INaOssFileOptStatusService{
    UPLOAD_ING("upload_ing", "上传中", "上传中","oss.file.upload_ing"),
    DONE("done", "done", "上传完成","oss.file.done"),
    ERROR("error", "error", "失败","oss.file.error"),
    REMOVE("remove", "remove", "删除","oss.file.remove"),
    QUERY_SUCCESS("query_success", "query_success", "查询成功","oss.file.query.success"),
    DATA_CONFIG_PARAMS_ERROR("data_config_params_error", "data_config_params_error", "配置参数错误","oss.file.data.config.error"),
    DATA_CHECK_ERROR("data_check_error", "data_check_error", "数据校验错误","oss.file.data.check.error"),
    ;


    private final String code;
    private final String enMsg;
    private final String zhMsg;
    private final String msgKey;

    private NaOssFileOptStatus(String code, String enMsg, String zhMsg,String msgKey)  {
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

    @Override
    public String getMsg() {
        if (Locale.SIMPLIFIED_CHINESE.getLanguage().equals(LocaleContextHolder.getLocale().getLanguage())) {
            return this.zhMsg;
        } else {
            return this.enMsg;
        }
    }

    @Override
    public String msgKey() {
        return msgKey;
    }

    @Override
    public String getLanguageMsg() {
        return "";
    }
}
