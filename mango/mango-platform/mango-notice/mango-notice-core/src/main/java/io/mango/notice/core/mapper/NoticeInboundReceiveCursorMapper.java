package io.mango.notice.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.notice.core.entity.NoticeInboundReceiveCursorEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NoticeInboundReceiveCursorMapper extends BaseMapper<NoticeInboundReceiveCursorEntity> {
}
