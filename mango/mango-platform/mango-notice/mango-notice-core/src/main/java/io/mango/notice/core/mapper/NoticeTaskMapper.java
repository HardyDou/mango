package io.mango.notice.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import io.mango.notice.core.entity.NoticeTaskEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface NoticeTaskMapper extends BaseMapper<NoticeTaskEntity> {

    @InterceptorIgnore(tenantLine = "true")
    @Select("select tenant_id from notice_task where id = #{id}")
    String selectTenantIdById(@Param("id") Long id);
}
