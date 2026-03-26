package io.mango.common.po;

import lombok.Data;

import java.io.Serializable;

/**
 * 请求参数基类
 * 所有 PO 必须继承此类
 *
 * @author Mango
 */
@Data
public class BasePO implements Serializable {

    private static final long serialVersionUID = 1L;
}
