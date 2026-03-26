package io.mango.common.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

/**
 * 分页返回
 * 继承 BaseVO
 *
 * @author Mango
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PageVO<T extends BaseVO> extends BaseVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 数据列表
     */
    private List<T> list;

    /**
     * 总数
     */
    private long total;

    /**
     * 当前页
     */
    private long page;

    /**
     * 每页大小
     */
    private long size;

    /**
     * 总页数
     */
    private long pages;

    public static <T extends BaseVO> PageVO<T> of(List<T> list, long total, long page, long size) {
        PageVO<T> vo = new PageVO<>();
        vo.setList(list);
        vo.setTotal(total);
        vo.setPage(page);
        vo.setSize(size);
        vo.setPages((total + size - 1) / size);
        return vo;
    }
}
