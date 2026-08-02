package io.mango.auth.api.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProviderAuthorizationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String authorizationUrl;
    private long expiresInSeconds;
}
