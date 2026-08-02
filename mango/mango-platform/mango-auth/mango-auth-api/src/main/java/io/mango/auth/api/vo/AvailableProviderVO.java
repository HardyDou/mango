package io.mango.auth.api.vo;

import io.mango.auth.api.enums.ExternalAuthProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvailableProviderVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "第三方登录提供方")
    private ExternalAuthProvider provider;
    @Schema(description = "第三方登录提供方显示名称")
    private String displayName;
}
