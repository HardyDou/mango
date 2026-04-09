package io.mango.rbac.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.rbac.core.entity.SysUserRole;
import org.apache.ibatis.annotations.Mapper;

/**
 * User-Role relationship mapper
 *
 * @author Mango
 */
@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {
}
