package io.mango.authorization.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.authorization.core.entity.Menu;
import org.apache.ibatis.annotations.Mapper;

/**
 * System menu mapper
 *
 * @author Mango
 */
@Mapper
public interface MenuMapper extends BaseMapper<Menu> {
}
