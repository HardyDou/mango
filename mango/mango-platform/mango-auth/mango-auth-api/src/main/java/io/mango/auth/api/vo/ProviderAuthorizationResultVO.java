package io.mango.auth.api.vo;

import io.mango.auth.api.enums.ProviderAuthorizationStatus;
import lombok.Data;

import java.io.Serializable;

@Data
public class ProviderAuthorizationResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private ProviderAuthorizationStatus status;
    private LoginVO login;
    private String bindingTicket;
    private String providerDisplayName;
    private long expiresInSeconds;
}
