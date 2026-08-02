package io.mango.identity.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContactCaptchaTicketVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "验证码校验凭据")
    private String key;
    @Schema(description = "脱敏后的验证码接收目标")
    private String target;
    @Schema(description = "验证码剩余有效秒数")
    private long expiresInSeconds;
}
