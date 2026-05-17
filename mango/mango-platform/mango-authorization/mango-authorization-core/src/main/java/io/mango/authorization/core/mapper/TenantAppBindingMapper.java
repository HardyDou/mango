package io.mango.authorization.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.authorization.core.entity.TenantAppBinding;
import org.apache.ibatis.annotations.Mapper;

/**
 * 租户应用开通关系 Mapper。
 */
@Mapper
public interface TenantAppBindingMapper extends BaseMapper<TenantAppBinding> {
}
