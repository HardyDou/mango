package io.mango.org.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("org_post")
public class PostEntity extends OrgBaseEntity {

    private String postName;

    private String postCode;

    private Integer postSort;

    private String postStatus;

    private String remark;

}
