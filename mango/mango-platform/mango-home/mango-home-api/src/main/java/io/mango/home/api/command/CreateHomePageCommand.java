package io.mango.home.api.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
public class CreateHomePageCommand implements Serializable {

    @NotBlank(message = "首页名称不能为空")
    @Size(max = 64, message = "首页名称长度不能超过64")
    private String name;

    @Size(max = 200000, message = "布局JSON长度不能超过200000")
    private String layoutJson;

    private Boolean setDefault;
}
