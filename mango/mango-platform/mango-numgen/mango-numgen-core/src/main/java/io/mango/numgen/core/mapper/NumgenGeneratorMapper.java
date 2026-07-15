package io.mango.numgen.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.numgen.core.entity.NumgenGeneratorEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NumgenGeneratorMapper extends BaseMapper<NumgenGeneratorEntity> {

    NumgenGeneratorEntity selectByIdIncludingDeleted(@Param("id") Long id);

    NumgenGeneratorEntity selectByTenantAndGenKeyIncludingDeleted(@Param("tenantId") String tenantId,
                                                                  @Param("genKey") String genKey);

    int physicalDeleteById(@Param("id") Long id);
}
