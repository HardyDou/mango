package {{basePackage}}.{{modulePackage}}.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.io.Serializable;

/**
 * {{aggregatePascal}}分页查询。
 */
@Schema(description = "{{aggregatePascal}}分页查询")
public class {{aggregatePascal}}PageQuery implements Serializable {

    @Schema(description = "页码，从 1 开始")
    @Min(value = 1, message = "页码不能小于 1")
    private int pageNo = 1;

    @Schema(description = "每页数量")
    @Min(value = 1, message = "每页数量不能小于 1")
    @Max(value = 200, message = "每页数量不能大于 200")
    private int pageSize = 20;

    @Schema(description = "{{aggregatePascal}}名称")
    private String name;

    public int getPageNo() {
        return pageNo;
    }

    public void setPageNo(int pageNo) {
        this.pageNo = pageNo;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
