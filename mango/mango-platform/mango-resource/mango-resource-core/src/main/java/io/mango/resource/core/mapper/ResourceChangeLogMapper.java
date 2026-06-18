package io.mango.resource.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.resource.core.entity.ResourceChangeLogEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ResourceChangeLogMapper extends BaseMapper<ResourceChangeLogEntity> {
}
