package io.mango.identity.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "第三方登录身份绑定")
public class ExternalIdentityBindingVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long userId;

    private String provider;

    private String corpId;

    private String externalUserId;

    private String displayName;

    private String bindSource;

    private String bindStatus;

    private LocalDateTime bindTime;

    private LocalDateTime lastLoginTime;
}
