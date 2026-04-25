package io.mango.authorization.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.authorization.core.entity.SubjectRoleBinding;
import org.apache.ibatis.annotations.Mapper;

/**
 * Subject-Role relationship mapper
 *
 * @author Mango
 */
@Mapper
public interface SubjectRoleBindingMapper extends BaseMapper<SubjectRoleBinding> {
}
