package io.mango.org.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("org_post")
public class PostEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

}
