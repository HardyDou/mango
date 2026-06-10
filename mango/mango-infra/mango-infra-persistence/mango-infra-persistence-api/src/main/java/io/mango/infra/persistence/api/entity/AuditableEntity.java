package io.mango.infra.persistence.api.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 带审计字段的基础实体。
 */
@Getter
@Setter
public class AuditableEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 创建人 ID。
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    /**
     * 创建时间。
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新人 ID。
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;

    /**
     * 更新时间。
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
