package io.mango.identity.api.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContactCaptchaTicketVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String key;
    private String target;
    private long expiresInSeconds;
}
