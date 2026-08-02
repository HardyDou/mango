package io.mango.auth.api.vo;

import io.mango.auth.api.enums.ExternalAuthProvider;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvailableProviderVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private ExternalAuthProvider provider;
    private String displayName;
}
