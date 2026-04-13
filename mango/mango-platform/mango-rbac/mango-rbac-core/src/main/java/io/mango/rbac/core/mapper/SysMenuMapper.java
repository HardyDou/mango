package io.mango.rbac.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.rbac.core.entity.SysMenu;
import org.apache.ibatis.annotations.Mapper;

/**
 * System menu mapper
 *
 * @author Mango
 */
@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {
}
