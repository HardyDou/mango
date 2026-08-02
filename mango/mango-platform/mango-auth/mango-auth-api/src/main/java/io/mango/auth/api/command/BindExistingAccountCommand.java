package io.mango.auth.api.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
public class BindExistingAccountCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank
    @Size(max = 256)
    private String bindingTicket;

    @NotBlank
    @Size(max = 100)
    private String username;

    @NotBlank
    @Size(max = 200)
    private String password;
}
