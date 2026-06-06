package io.mango.job.starter.powerjob;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * PowerJob DFS MySQL 文件 Mapper，只读用于读取原生实例日志。
 */
@Mapper
@InterceptorIgnore(tenantLine = "true")
public interface PowerJobFileMapper extends BaseMapper<PowerJobFileEntity> {
}
