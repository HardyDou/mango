package io.mango.infra.persistence.api.query;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 持久化分页查询结果。
 *
 * @param <T> 数据类型。
 */
@Getter
@Setter
public class PersistencePageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 数据列表。
     */
    private List<T> records = new ArrayList<>();

    /**
     * 总记录数。
     */
    private long total;

    /**
     * 当前页。
     */
    private long page;

    /**
     * 每页大小。
     */
    private long size;

    /**
     * 总页数。
     */
    private long pages;

    public List<T> getRecords() {
        return records == null ? new ArrayList<>() : new ArrayList<>(records);
    }

    public void setRecords(List<T> records) {
        this.records = records == null ? new ArrayList<>() : new ArrayList<>(records);
    }

    public static <T> PersistencePageResult<T> of(List<T> records, long total, long page, long size) {
        PersistencePageResult<T> result = new PersistencePageResult<>();
        result.setRecords(records);
        result.setTotal(total);
        result.setPage(page);
        result.setSize(size);
        result.setPages(size <= 0 ? 0 : (total + size - 1) / size);
        return result;
    }
}
