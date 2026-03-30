package io.mango.common.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 分页返回
 * 继承 BaseVO
 *
 * @author Mango
 */
@Data
@EqualsAndHashCode(callSuper = true, exclude = "list")
public class PageVO<T extends BaseVO> extends BaseVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 数据列表
     */
    private List<T> list = new ArrayList<>();

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

    /**
     * 获取列表副本，防止外部修改内部状态
     */
    public List<T> getList() {
        return list == null ? new ArrayList<>() : new ArrayList<>(list);
    }

    /**
     * 设置列表副本，防止外部修改内部状态
     */
    public void setList(List<T> list) {
        this.list = list == null ? new ArrayList<>() : new ArrayList<>(list);
    }

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
