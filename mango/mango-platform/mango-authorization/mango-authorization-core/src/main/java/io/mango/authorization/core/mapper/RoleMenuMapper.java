package io.mango.authorization.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.authorization.core.entity.RoleMenuEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * RoleEntity-MenuEntity relationship mapper
 *
 * @author Mango
 */
@Mapper
public interface RoleMenuMapper extends BaseMapper<RoleMenuEntity> {
}
