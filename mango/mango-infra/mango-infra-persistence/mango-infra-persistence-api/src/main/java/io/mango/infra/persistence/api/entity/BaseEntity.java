package io.mango.infra.persistence.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 基础实体。
 */
@Getter
@Setter
public class BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键。
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 主键类型。
     * <p>
     * Mango 业务表默认使用 Long 雪花 ID；特殊实体可覆盖。
     */
    public Class<?> idType() {
        return Long.class;
    }
}
