package io.mango.payment.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "支付方式分类视图")
public class PaymentMethodCategoryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "分类 ID")
    private Long id;

    @Schema(description = "分类编码")
    private String categoryCode;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "层级：1-一级，2-二级，3-三级")
    private Integer level;

    @Schema(description = "父级分类 ID")
    private Long parentId;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "子分类")
    private List<PaymentMethodCategoryVO> children = new ArrayList<>();
}
