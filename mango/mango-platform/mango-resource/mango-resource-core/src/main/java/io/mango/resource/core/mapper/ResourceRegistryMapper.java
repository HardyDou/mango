package io.mango.resource.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.resource.core.entity.ResourceRegistryEntity;
import io.mango.resource.core.sync.ResourceRegistryLookupKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ResourceRegistryMapper extends BaseMapper<ResourceRegistryEntity> {

    ResourceRegistryEntity selectByResourceId(@Param("resourceId") String resourceId);

    ResourceRegistryEntity selectByTypeAndBizKey(@Param("resourceType") String resourceType,
                                                 @Param("bizKey") String bizKey);

    List<ResourceRegistryEntity> selectByResourceIds(@Param("resourceIds") List<String> resourceIds);

    List<ResourceRegistryEntity> selectByTypeAndBizKeys(
            @Param("lookupKeys") List<ResourceRegistryLookupKey> lookupKeys);

    List<ResourceRegistryEntity> selectByModule(@Param("moduleCode") String moduleCode);

    List<ResourceRegistryEntity> selectBySourceAndModule(@Param("appCode") String appCode,
                                                         @Param("serviceCode") String serviceCode,
                                                         @Param("moduleCode") String moduleCode);

    List<ResourceRegistryEntity> selectBySourceAndModules(@Param("appCode") String appCode,
                                                          @Param("serviceCode") String serviceCode,
                                                          @Param("moduleCodes") List<String> moduleCodes);
}
