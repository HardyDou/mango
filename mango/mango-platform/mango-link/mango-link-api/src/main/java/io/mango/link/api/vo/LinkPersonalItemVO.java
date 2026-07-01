package io.mango.link.api.vo;

import io.mango.link.api.enums.LinkOpenMode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 个人网址返回对象。
 */
@Data
@Schema(description = "个人网址返回对象")
public class LinkPersonalItemVO {

    private Long id;
    private Long categoryId;
    private String categoryName;
    private String name;
    private String url;
    private String summary;
    private String iconUrl;
    private List<String> tags;
    private String remark;
    private LinkOpenMode openMode;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
