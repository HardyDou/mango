package io.mango.auth.api.vo;

import io.mango.common.vo.BaseVO;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Login request VO
 *
 * @author Mango
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class LoginRequest extends BaseVO {

    private static final long serialVersionUID = 1L;

    /**
     * Username
     */
    @NotBlank(message = "Username is required")
    private String username;

    /**
     * Password
     */
    @NotBlank(message = "Password is required")
    private String password;

    /**
     * Captcha code
     */
    private String captchaCode;

    /**
     * Captcha key (for captcha validation)
     */
    private String captchaKey;
}
