package io.mango.infra.persistence.api.entity;

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
    private Long createdBy;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 更新人 ID。
     */
    private Long updatedBy;

    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}
