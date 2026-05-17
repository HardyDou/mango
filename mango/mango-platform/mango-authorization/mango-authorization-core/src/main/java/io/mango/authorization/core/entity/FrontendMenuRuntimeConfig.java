package io.mango.authorization.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 前端菜单运行配置。
 * <p>
 * 以授权菜单 menuId 为关联键，独立保存页面运行类型和外部地址，不改动 authorization_menu 表结构。
 */
@Data
@TableName("frontend_menu_runtime_config")
public class FrontendMenuRuntimeConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 菜单运行配置 ID。 */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long configId;

    /** 授权菜单 ID。 */
    private Long menuId;

    /** 授权应用编码。 */
    private String appCode;

    /** 页面运行类型：LOCAL_ROUTE/MICRO_ROUTE/IFRAME/EXTERNAL_LINK/BUTTON。 */
    private String pageType;

    /** iframe 或外链地址。 */
    private String externalUrl;

    /** 创建时间。 */
    private LocalDateTime createTime;

    /** 更新时间。 */
    private LocalDateTime updateTime;
}
