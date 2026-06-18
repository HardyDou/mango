package io.mango.resource.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.resource.core.entity.ResourceRegistryEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ResourceRegistryMapper extends BaseMapper<ResourceRegistryEntity> {

    ResourceRegistryEntity selectByResourceId(@Param("resourceId") String resourceId);

    ResourceRegistryEntity selectByTypeAndBizKey(@Param("resourceType") String resourceType,
                                                 @Param("bizKey") String bizKey);

    List<ResourceRegistryEntity> selectByModule(@Param("moduleCode") String moduleCode);
}
