package io.mango.home.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/** 首页管理用户候选项查询。 */
@Data
@Schema(description = "首页管理用户候选项查询")
public class HomeUserOptionQuery implements Serializable {

    @Schema(description = "姓名或账号关键字")
    @Size(max = 100, message = "用户关键字最多100个字符")
    private String keyword;

    @Schema(description = "返回数量，默认50，最多200")
    @Min(value = 1, message = "返回数量至少为1")
    @Max(value = 200, message = "返回数量不能超过200")
    private Long size = 50L;
}
