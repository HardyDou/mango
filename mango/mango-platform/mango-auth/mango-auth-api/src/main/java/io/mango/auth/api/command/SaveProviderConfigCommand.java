package io.mango.auth.api.command;

import io.mango.auth.api.enums.ExternalAuthProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class SaveProviderConfigCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    @NotBlank
    @Size(max = 64)
    private String appCode;

    @NotNull
    private ExternalAuthProvider provider;

    @Size(max = 128)
    private String clientId;

    @Size(max = 128)
    private String providerTenantId;

    @Size(max = 64)
    private String agentId;

    @Size(max = 512)
    private String secret;

    @NotNull
    @Size(min = 1, max = 10)
    private List<@NotBlank @Size(max = 500) String> redirectUris = new ArrayList<>();

    @NotNull
    private Boolean enabled;
}
