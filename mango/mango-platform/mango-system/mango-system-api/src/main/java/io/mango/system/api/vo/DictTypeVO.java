package io.mango.system.api.vo;

import lombok.Data;

@Data
public class DictTypeVO {
    private Long id;
    private String dictType;
    private String dictName;
    private Integer status;
    private String remark;
}
