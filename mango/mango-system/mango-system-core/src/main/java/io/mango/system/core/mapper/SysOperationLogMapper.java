package io.mango.system.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.system.core.entity.SysOperationLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysOperationLogMapper extends BaseMapper<SysOperationLog> {
}
