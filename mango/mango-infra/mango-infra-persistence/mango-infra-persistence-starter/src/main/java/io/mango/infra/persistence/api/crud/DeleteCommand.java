package io.mango.infra.persistence.api.crud;

import lombok.Data;

/**
 * 标准删除命令。
 */
@Data
public class DeleteCommand {

    /**
     * 主键。
     */
    private Long id;
}
