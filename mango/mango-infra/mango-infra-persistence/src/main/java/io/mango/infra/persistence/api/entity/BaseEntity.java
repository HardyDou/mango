package io.mango.infra.persistence.api.entity;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 基础实体。
 *
 * @param <ID> 主键类型。
 */
@Getter
@Setter
public class BaseEntity<ID extends Serializable> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键。
     */
    private ID id;
}
