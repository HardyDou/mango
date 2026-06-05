package io.mango.identity.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.identity.core.entity.ExternalIdentityBindingEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ExternalIdentityBindingMapper extends BaseMapper<ExternalIdentityBindingEntity> {
}
