package io.mango.captcha.api.spi;

/**
 * 邮件发送接口
 * 支持多供应商配置
 *
 * @author Mango
 */
public interface EmailProvider {

    /**
     * 发送邮件
     *
     * @param to      收件人邮箱
     * @param subject 主题
     * @param content 内容
     * @return true-发送成功
     */
    boolean send(String to, String subject, String content);

    /**
     * 获取供应商名称
     *
     * @return 供应商名称
     */
    String getName();
}
