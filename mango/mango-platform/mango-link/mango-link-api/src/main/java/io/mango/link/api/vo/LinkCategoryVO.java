package io.mango.link.api.vo;

import io.mango.link.api.enums.LinkCategoryScope;
import io.mango.link.api.enums.LinkStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 网址分类返回对象。
 */
@Data
@Schema(description = "网址分类返回对象")
public class LinkCategoryVO {

    private Long id;
    private String name;
    private LinkCategoryScope scope;
    private Long ownerUserId;
    private Integer sortNo;
    private LinkStatus status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
