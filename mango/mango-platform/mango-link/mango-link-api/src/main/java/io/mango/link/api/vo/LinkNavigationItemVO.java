package io.mango.link.api.vo;

import io.mango.link.api.enums.LinkOpenMode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 用户侧网址返回对象。
 */
@Data
@Schema(description = "用户侧网址返回对象")
public class LinkNavigationItemVO {

    private Long id;
    private Long categoryId;
    private String categoryName;
    private String name;
    private String url;
    private String summary;
    private String iconUrl;
    private List<String> tags;
    private LinkOpenMode openMode;
    private Boolean recommended;
    private Integer sortNo;
    private Boolean favorited;
}
