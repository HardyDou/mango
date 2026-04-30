package io.mango.infra.persistence.api.query;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 持久化分页查询参数。
 */
@Getter
@Setter
public class PersistencePageQuery implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final long DEFAULT_PAGE = 1L;
    private static final long DEFAULT_SIZE = 10L;
    private static final long MAX_SIZE = 500L;

    /**
     * 当前页，从 1 开始。
     */
    private long page = DEFAULT_PAGE;

    /**
     * 每页大小。
     */
    private long size = DEFAULT_SIZE;

    /**
     * 排序条件。
     */
    private List<PersistenceSort> sorts = new ArrayList<>();

    public long getPage() {
        return Math.max(page, DEFAULT_PAGE);
    }

    public long getSize() {
        if (size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    public List<PersistenceSort> getSorts() {
        return sorts == null ? new ArrayList<>() : new ArrayList<>(sorts);
    }

    public void setSorts(List<PersistenceSort> sorts) {
        this.sorts = sorts == null ? new ArrayList<>() : new ArrayList<>(sorts);
    }
}
