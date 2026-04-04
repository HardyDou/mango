package io.mango.message.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.message.core.entity.SysMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysMessageMapper extends BaseMapper<SysMessage> {
}
