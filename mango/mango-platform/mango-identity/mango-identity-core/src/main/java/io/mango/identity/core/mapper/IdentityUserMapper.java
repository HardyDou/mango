package io.mango.identity.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.identity.core.entity.IdentityUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * Identity user mapper
 *
 * @author Mango
 */
@Mapper
public interface IdentityUserMapper extends BaseMapper<IdentityUser> {
}
