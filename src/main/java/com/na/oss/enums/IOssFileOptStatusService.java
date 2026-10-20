package com.na.oss.enums;

/**
 * 文件操作状态接口。
 * <p>
 * 统一约束状态码、英文/中文提示、国际化消息 key 的获取方式，
 * 便于业务方对接自定义状态枚举或国际化框架。
 * </p>
 */
public interface IOssFileOptStatusService {

    /**
     * 获取状态编码（用于接口返回或持久化）。
     *
     * @return 状态编码
     */
    String code();

    /**
     * 获取英文提示信息。
     *
     * @return 英文提示
     */
    String enMsg();

    /**
     * 获取中文提示信息。
     *
     * @return 中文提示
     */
    String zhMsg();

    /**
     * 根据当前请求语言环境获取提示信息。
     *
     * @return 本地化提示信息
     */
    String getMsg();

    /**
     * 获取国际化消息 key（形如 {@code oss.file.done}）。
     *
     * @return 消息 key
     */
    String msgKey();

    /**
     * 获取国际化后的提示信息。
     * <p>默认返回 {@link #getMsg()}，接入 Spring MessageSource 时可覆盖实现。</p>
     *
     * @return 国际化提示信息
     */
    String getLanguageMsg();
}
