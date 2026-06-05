package io.mango.job.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.job.core.entity.MangoJobWorkerSnapshotEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * Job Worker 快照 Mapper。
 */
@Mapper
public interface MangoJobWorkerSnapshotMapper extends BaseMapper<MangoJobWorkerSnapshotEntity> {
}
