package io.mango.common.po;

import lombok.Data;

/**
 * 分页请求参数
 *
 * @author Mango
 */
@Data
public class PageQuery {

    private static final long serialVersionUID = 1L;

    /**
     * 当前页（从 1 开始）
     */
    private long page = 1;

    /**
     * 每页大小
     */
    private long size = 10;

    /**
     * 获取当前页（最小为 1）
     */
    public long getPage() {
        return Math.max(page, 1);
    }

    /**
     * 获取每页大小（最小为 1，最大为 100）
     */
    public long getSize() {
        if (size <= 0) {
            return 10;
        }
        return Math.min(size, 100);
    }
}
