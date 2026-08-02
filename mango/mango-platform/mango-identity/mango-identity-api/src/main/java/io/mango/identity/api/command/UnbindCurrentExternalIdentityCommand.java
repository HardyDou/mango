package io.mango.identity.api.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
public class UnbindCurrentExternalIdentityCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull
    @Positive
    private Long bindingId;

    @NotBlank
    @Size(max = 200)
    private String currentPassword;
}
