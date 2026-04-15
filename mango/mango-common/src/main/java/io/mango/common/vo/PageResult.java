package io.mango.common.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 分页返回。
 *
 * @param <T> 列表元素类型。
 * @author Mango
 */
@Data
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 数据列表。 */
    private List<T> list = new ArrayList<>();

    /** 总数。 */
    private long total;

    /** 当前页。 */
    private long page;

    /** 每页大小。 */
    private long size;

    /** 总页数。 */
    private long pages;

    /**
     * 返回列表副本，防止外部修改内部状态。
     *
     * @return 列表副本。
     */
    public List<T> getList() {
        if (list == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(list);
    }

    /**
     * 设置列表副本，防止外部修改内部状态。
     *
     * @param list 列表数据。
     */
    public void setList(List<T> list) {
        if (list == null) {
            this.list = new ArrayList<>();
            return;
        }
        this.list = new ArrayList<>(list);
    }

    /**
     * 创建分页结果。
     *
     * @param list 列表数据。
     * @param total 总记录数。
     * @param page 当前页。
     * @param size 每页大小。
     * @return 分页结果。
     */
    public static <T> PageResult<T> of(List<T> list, long total, long page, long size) {
        PageResult<T> result = new PageResult<>();
        result.setList(list);
        result.setTotal(total);
        result.setPage(page);
        result.setSize(size);
        result.setPages((total + size - 1) / size);
        return result;
    }
}
