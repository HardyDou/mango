package io.mango.home.api.command;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
public class HomePageIdCommand implements Serializable {

    @NotNull(message = "首页ID不能为空")
    private Long id;
}
