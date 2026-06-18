package io.mango.domain.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.domain.core.entity.DomainEntity;
import org.apache.ibatis.annotations.Param;

/**
 * 业务域 Mapper。
 */
public interface DomainMapper extends BaseMapper<DomainEntity> {

    /**
     * 查询指定租户与编码的业务域，包含已逻辑删除数据。
     *
     * @param tenantId 租户标识。
     * @param domainCode 业务域编码。
     * @return 业务域。
     */
    DomainEntity selectByTenantAndCodeIncludingDeleted(@Param("tenantId") String tenantId,
                                                       @Param("domainCode") String domainCode);

    /**
     * 根据 ID 查询业务域，包含已逻辑删除数据。
     *
     * @param id 业务域 ID。
     * @return 业务域。
     */
    DomainEntity selectByIdIncludingDeleted(@Param("id") Long id);

    /**
     * 根据 ID 物理删除业务域。
     *
     * @param id 业务域 ID。
     * @return 删除行数。
     */
    int physicalDeleteById(@Param("id") Long id);
}
