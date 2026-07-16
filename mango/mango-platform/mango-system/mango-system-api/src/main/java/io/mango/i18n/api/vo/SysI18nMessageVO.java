package io.mango.i18n.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "国际化条目视图")
public class SysI18nMessageVO {
    @Schema(description = "主键 ID")
    private Long id;
    @Schema(description = "名称")
    private String name;
    @Schema(description = "简体中文文案")
    private String zhCn;
    @Schema(description = "英文文案")
    private String en;
    @Schema(description = "说明")
    private String description;
}
