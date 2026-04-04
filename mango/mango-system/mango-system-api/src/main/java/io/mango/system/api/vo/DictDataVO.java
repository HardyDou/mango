package io.mango.system.api.vo;

import lombok.Data;

@Data
public class DictDataVO {
    private Long id;
    private String dictType;
    private String dictLabel;
    private String dictValue;
    private Integer sort;
    private Integer status;
}
