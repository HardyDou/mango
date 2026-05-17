package io.mango.file.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.file.core.entity.FileSettings;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件中心运行时配置 Mapper。
 */
@Mapper
public interface FileSettingsMapper extends BaseMapper<FileSettings> {
}
