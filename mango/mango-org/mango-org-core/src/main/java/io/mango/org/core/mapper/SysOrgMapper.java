package io.mango.org.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.org.api.entity.SysOrg;
import org.apache.ibatis.annotations.Mapper;

/**
 * Organization mapper
 *
 * @author Mango
 */
@Mapper
public interface SysOrgMapper extends BaseMapper<SysOrg> {
}
