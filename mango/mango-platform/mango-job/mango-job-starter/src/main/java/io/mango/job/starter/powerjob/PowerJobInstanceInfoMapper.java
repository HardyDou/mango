package io.mango.job.starter.powerjob;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * PowerJob 实例 Mapper，只读用于导入调度实例。
 */
@Mapper
@InterceptorIgnore(tenantLine = "true")
public interface PowerJobInstanceInfoMapper extends BaseMapper<PowerJobInstanceInfoEntity> {
}
