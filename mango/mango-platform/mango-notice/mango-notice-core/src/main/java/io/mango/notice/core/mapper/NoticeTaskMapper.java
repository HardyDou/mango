package io.mango.notice.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import io.mango.notice.core.entity.NoticeTaskEntity;
import org.apache.ibatis.annotations.Param;

@org.apache.ibatis.annotations.Mapper
public interface NoticeTaskMapper extends BaseMapper<NoticeTaskEntity> {

    @InterceptorIgnore(tenantLine = "true")
    String selectTenantIdById(@Param("id") Long id);
}
