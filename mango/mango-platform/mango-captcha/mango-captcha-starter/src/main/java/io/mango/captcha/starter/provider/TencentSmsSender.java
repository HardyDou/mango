package io.mango.captcha.starter.provider;

import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.sms.v20210111.SmsClient;
import com.tencentcloudapi.sms.v20210111.models.SendSmsRequest;
import com.tencentcloudapi.sms.v20210111.models.SendSmsResponse;
import com.tencentcloudapi.sms.v20210111.models.SendStatus;
import io.mango.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;

/**
 * 腾讯云短信 SDK 调用封装。
 *
 * <p>把验证码发送参数映射为 {@code SendSms} 请求，首个发送状态为 {@code OK} 视为成功；
 * 其它状态与 SDK 异常统一转换为 {@link io.mango.captcha.api.enums.CaptchaCode#CAPTCHA_SEND_FAILED}。
 * 日志不输出手机号明文与凭证值。</p>
 *
 * @author Mango
 */
@Slf4j
public class TencentSmsSender {

    static final String SUCCESS_CODE = "OK";
    static final String DEFAULT_ENDPOINT = "sms.tencentcloudapi.com";
    private static final String PROVIDER_NAME = "tencent";

    /**
     * 发送验证码短信。
     *
     * @param message 发送参数
     * @return 发送成功返回 {@code true}，失败抛出业务异常
     */
    public boolean send(TencentSmsMessage message) {
        try {
            SendSmsResponse response = createClient(message).SendSms(buildRequest(message));
            SendStatus[] statuses = response == null ? null : response.getSendStatusSet();
            if (isSuccess(statuses)) {
                log.info("腾讯云短信发送成功: provider={}, templateCode={}, serialNo={}",
                        PROVIDER_NAME, message.templateCode(), statuses[0].getSerialNo());
                return true;
            }
            log.error("腾讯云短信发送失败: provider={}, templateCode={}, code={}, message={}",
                    PROVIDER_NAME, message.templateCode(), firstCode(statuses), firstMessage(statuses));
        } catch (BizException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("腾讯云短信接口调用异常: provider={}, templateCode={}, error={}", PROVIDER_NAME,
                    message.templateCode(), exception.getMessage(), exception);
        }
        throw SmsProviderSupport.sendFailed();
    }

    /**
     * 创建腾讯云短信客户端。
     *
     * @param message 发送参数
     * @return 腾讯云短信客户端
     */
    protected SmsClient createClient(TencentSmsMessage message) {
        HttpProfile httpProfile = new HttpProfile();
        httpProfile.setEndpoint(message.endpoint());
        ClientProfile clientProfile = new ClientProfile();
        clientProfile.setHttpProfile(httpProfile);
        return new SmsClient(new Credential(message.secretId(), message.secretKey()),
                message.region(), clientProfile);
    }

    SendSmsRequest buildRequest(TencentSmsMessage message) {
        SendSmsRequest request = new SendSmsRequest();
        request.setPhoneNumberSet(new String[]{message.countryCode() + message.mobile()});
        request.setSmsSdkAppId(message.sdkAppId());
        request.setSignName(message.signName());
        request.setTemplateId(message.templateCode());
        request.setTemplateParamSet(new String[]{message.templateParam()});
        return request;
    }

    private boolean isSuccess(SendStatus[] statuses) {
        return statuses != null && statuses.length > 0 && SUCCESS_CODE.equalsIgnoreCase(statuses[0].getCode());
    }

    private String firstCode(SendStatus[] statuses) {
        return statuses == null || statuses.length == 0 ? null : statuses[0].getCode();
    }

    private String firstMessage(SendStatus[] statuses) {
        return statuses == null || statuses.length == 0 ? null : statuses[0].getMessage();
    }
}
