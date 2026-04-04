package io.mango.user.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.user.api.po.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * User mapper
 *
 * @author Mango
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
