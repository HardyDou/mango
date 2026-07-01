package io.mango.link.api.vo;

import io.mango.link.api.enums.LinkNavigationSource;
import io.mango.link.api.enums.LinkOpenMode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 公开网址返回对象。
 */
@Data
@Schema(description = "公开网址返回对象")
public class LinkPublicItemVO {

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
    private LinkNavigationSource source;
    private String redirectUrl;
}
