package io.mango.auth.api.command;

import io.mango.auth.api.enums.ExternalAuthProvider;
import io.mango.auth.api.enums.ProviderAuthorizationIntent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
public class StartProviderAuthorizationCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank
    @Size(max = 64)
    private String tenantId;

    @NotBlank
    @Size(max = 64)
    private String appCode;

    @NotNull
    private ExternalAuthProvider provider;

    @NotNull
    private ProviderAuthorizationIntent intent;

    @NotBlank
    @Size(max = 500)
    private String redirectUri;
}
