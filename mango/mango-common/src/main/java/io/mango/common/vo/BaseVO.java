package io.mango.common.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 返回参数基类
 * 所有 VO 必须继承此类
 *
 * @author Mango
 */
@Data
public class BaseVO implements Serializable {

    private static final long serialVersionUID = 1L;
}
