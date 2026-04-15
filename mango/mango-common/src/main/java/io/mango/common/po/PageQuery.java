package io.mango.common.po;

import lombok.Data;

/**
 * 分页请求参数。
 *
 * @author Mango
 */
@Data
public class PageQuery {

    private static final long serialVersionUID = 1L;
    private static final long DEFAULT_PAGE = 1L;
    private static final long DEFAULT_SIZE = 10L;
    private static final long MAX_SIZE = 100L;

    /** 当前页，从 1 开始。 */
    private long page = DEFAULT_PAGE;

    /** 每页大小。 */
    private long size = DEFAULT_SIZE;

    /**
     * 返回规范化后的当前页。
     *
     * @return 最小为 1 的当前页。
     */
    public long getPage() {
        return Math.max(page, DEFAULT_PAGE);
    }

    /**
     * 返回规范化后的分页大小。
     *
     * @return 最小为 1、最大为 100 的分页大小。
     */
    public long getSize() {
        if (size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }
}
