package io.mango.notice.core.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.notice.core.entity.NoticeInboundMessageEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NoticeInboundMessageMapper extends BaseMapper<NoticeInboundMessageEntity> {

    @InterceptorIgnore(tenantLine = "true")
    List<NoticeInboundMessageEntity> selectDueBroadcasts(@Param("limit") int limit);
}
