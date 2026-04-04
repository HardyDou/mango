package io.mango.captcha.api.spi;

/**
 * 短信发送接口
 * 支持多供应商配置
 *
 * @author Mango
 */
public interface SmsProvider {

    /**
     * 发送短信
     *
     * @param mobile      手机号
     * @param templateCode 模板编码
     * @param params      参数
     * @return true-发送成功
     */
    boolean send(String mobile, String templateCode, String... params);

    /**
     * 获取供应商名称
     *
     * @return 供应商名称
     */
    String getName();
}
