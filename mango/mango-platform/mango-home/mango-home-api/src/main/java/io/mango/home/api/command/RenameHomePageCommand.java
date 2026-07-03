package io.mango.home.api.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
public class RenameHomePageCommand implements Serializable {

    @NotNull(message = "首页ID不能为空")
    private Long id;

    @NotBlank(message = "首页名称不能为空")
    @Size(max = 64, message = "首页名称长度不能超过64")
    private String name;
}
