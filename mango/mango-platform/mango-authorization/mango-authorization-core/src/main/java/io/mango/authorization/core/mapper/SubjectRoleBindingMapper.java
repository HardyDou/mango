package io.mango.authorization.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.authorization.core.entity.SubjectRoleBindingEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * Subject-RoleEntity relationship mapper
 *
 * @author Mango
 */
@Mapper
public interface SubjectRoleBindingMapper extends BaseMapper<SubjectRoleBindingEntity> {
}
