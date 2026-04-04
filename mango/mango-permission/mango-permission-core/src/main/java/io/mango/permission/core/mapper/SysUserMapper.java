package io.mango.permission.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.permission.core.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * System user mapper
 *
 * @author Mango
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
