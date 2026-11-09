package io.mango.integration.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.integration.core.entity.IntegrationCredentialEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * IntegrationCredential Mapper。
 */
@Mapper
public interface IntegrationCredentialMapper extends BaseMapper<IntegrationCredentialEntity> {
}
