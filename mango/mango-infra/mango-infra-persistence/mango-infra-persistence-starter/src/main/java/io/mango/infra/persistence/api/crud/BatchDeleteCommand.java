package io.mango.infra.persistence.api.crud;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 标准批量删除命令。
 */
@Data
public class BatchDeleteCommand {

    /**
     * 主键列表。
     */
    private List<Long> ids = new ArrayList<>();
}
