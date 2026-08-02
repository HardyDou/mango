package io.mango.identity.api.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
public class SendContactCaptchaCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank
    @Pattern(regexp = "PHONE|EMAIL")
    private String contactType;

    @NotBlank
    @Size(max = 100)
    private String target;
}
