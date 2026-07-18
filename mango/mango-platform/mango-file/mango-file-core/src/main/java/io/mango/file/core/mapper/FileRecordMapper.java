package io.mango.file.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.file.core.entity.FileRecordEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件记录 Mapper。
 */
@Mapper
public interface FileRecordMapper extends BaseMapper<FileRecordEntity> {
}
