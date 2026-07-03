package io.mango.home.api.command;

import io.mango.home.api.vo.HomeTemplateAuthorizationItem;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "保存首页模板授权命令")
public class SaveHomeTemplateAuthorizationsCommand implements Serializable {

    @NotNull(message = "模板ID不能为空")
    @Schema(description = "模板ID")
    private Long templateId;

    @Valid
    @Schema(description = "授权项列表")
    private List<HomeTemplateAuthorizationItem> authorizations = new ArrayList<>();
}
