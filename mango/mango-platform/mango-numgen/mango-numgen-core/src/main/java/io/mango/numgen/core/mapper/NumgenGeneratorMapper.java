package io.mango.numgen.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.numgen.core.entity.NumgenGenerator;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NumgenGeneratorMapper extends BaseMapper<NumgenGenerator> {

    NumgenGenerator selectByIdIncludingDeleted(@Param("id") Long id);

    NumgenGenerator selectByTenantAndGenKeyIncludingDeleted(@Param("tenantId") Long tenantId,
                                                            @Param("genKey") String genKey);

    int physicalDeleteById(@Param("id") Long id);
}
