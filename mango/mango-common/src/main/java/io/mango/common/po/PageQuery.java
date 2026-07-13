package io.mango.common.po;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 分页请求参数。
 *
 * @author Mango
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "分页请求参数")
public class PageQuery extends Query {

    private static final long serialVersionUID = 1L;
    private static final long DEFAULT_PAGE = 1L;
    private static final long DEFAULT_SIZE = 10L;
    private static final long MAX_SIZE = 500L;

    /** 当前页，从 1 开始。 */
    @Schema(description = "当前页，从 1 开始")
    @Min(value = 1, message = "当前页必须大于等于1")
    private long page = DEFAULT_PAGE;

    /** 每页大小。 */
    @Schema(description = "每页大小，最大 500")
    @Min(value = 1, message = "每页大小必须大于等于1")
    @Max(value = MAX_SIZE, message = "每页大小不能超过500")
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
     * @return 最小为 1、最大为 500 的分页大小。
     */
    public long getSize() {
        if (size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }
}
