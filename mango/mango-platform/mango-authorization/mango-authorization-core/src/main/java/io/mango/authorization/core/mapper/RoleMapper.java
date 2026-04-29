package io.mango.authorization.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.authorization.core.entity.Role;
import org.apache.ibatis.annotations.Mapper;

/**
 * System role mapper
 *
 * @author Mango
 */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {
}
