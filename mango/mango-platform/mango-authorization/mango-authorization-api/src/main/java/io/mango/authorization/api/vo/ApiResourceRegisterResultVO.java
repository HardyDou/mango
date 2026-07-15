package io.mango.authorization.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
/**
 * API 资源注册结果。
 *
 * @author hardy
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "API资源注册结果")
public class ApiResourceRegisterResultVO {

    @Schema(description = "扫描到的资源数量")
    private int scanned;

    @Schema(description = "新增资源数量")
    private int created;

    @Schema(description = "更新资源数量")
    private int updated;

    public static ApiResourceRegisterResultVO empty() {
        return new ApiResourceRegisterResultVO(0, 0, 0);
    }

    public int scanned() {
        return scanned;
    }

    public int created() {
        return created;
    }

    public int updated() {
        return updated;
    }
}
