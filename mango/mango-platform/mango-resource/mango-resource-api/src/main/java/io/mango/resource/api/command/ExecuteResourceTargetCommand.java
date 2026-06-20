package io.mango.resource.api.command;

import io.mango.resource.api.model.ResourceDeclaration;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 资源目标模块内部执行命令。
 */
@Data
public class ExecuteResourceTargetCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotEmpty(message = "资源声明不能为空")
    @Valid
    private List<ResourceDeclaration> declarations = new ArrayList<>();

    @Valid
    private List<ResourceDeclaration> completeBatch = new ArrayList<>();
}
