package io.mango.identity.api.command;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
public class UpdateCurrentUserProfileCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Size(max = 100)
    private String nickname;

    @Size(max = 500)
    private String avatar;

    @Size(max = 100)
    private String realName;

    @Size(max = 32)
    private String documentType;

    @Size(max = 128)
    private String documentNumber;
}
