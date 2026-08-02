package io.mango.auth.api.vo;

import io.mango.auth.api.enums.ExternalAuthProvider;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class ProviderConfigVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String appCode;
    private ExternalAuthProvider provider;
    private String clientId;
    private String providerTenantId;
    private String agentId;
    private List<String> redirectUris = new ArrayList<>();
    private Boolean enabled;
    private Boolean secretConfigured;
    private Boolean complete;
    private LocalDateTime updatedAt;
}
