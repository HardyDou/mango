package io.mango.template.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.template.core.entity.TemplateRenderRecordEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 模板渲染记录 Mapper。
 */
@Mapper
public interface TemplateRenderRecordMapper extends BaseMapper<TemplateRenderRecordEntity> {
}
