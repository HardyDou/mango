package io.mango.authorization.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.authorization.core.entity.SysRoleMenu;
import org.apache.ibatis.annotations.Mapper;

/**
 * Role-Menu relationship mapper
 *
 * @author Mango
 */
@Mapper
public interface SysRoleMenuMapper extends BaseMapper<SysRoleMenu> {
}
