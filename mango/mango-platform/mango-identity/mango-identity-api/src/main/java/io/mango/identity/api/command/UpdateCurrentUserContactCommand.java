package io.mango.identity.api.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
public class UpdateCurrentUserContactCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank
    @Pattern(regexp = "PHONE|EMAIL")
    private String contactType;

    @NotBlank
    @Size(max = 100)
    private String target;

    @NotBlank
    @Size(max = 200)
    private String currentPassword;

    @NotBlank
    @Size(max = 256)
    private String captchaKey;

    @NotBlank
    @Size(max = 256)
    private String captchaCode;
}
