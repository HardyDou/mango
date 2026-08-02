package io.mango.auth.api.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
public class CompleteProviderAuthorizationCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank
    @Size(max = 256)
    private String state;

    @NotBlank
    @Size(max = 1024)
    private String code;
}
