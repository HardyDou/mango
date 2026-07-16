package io.mango.i18n.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.TenantEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("sys_i18n")
public class SysI18nEntity extends TenantEntity {
    private String name;
    @TableField("zh_cn")
    private String zhCn;
    private String en;
    private String description;
}
