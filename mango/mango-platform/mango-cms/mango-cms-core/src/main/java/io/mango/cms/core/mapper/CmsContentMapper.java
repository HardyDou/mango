package io.mango.cms.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.cms.core.entity.CmsContentEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface CmsContentMapper extends BaseMapper<CmsContentEntity> {

    IPage<CmsContentEntity> selectPublicPage(Page<CmsContentEntity> page,
                                             @Param("tenantId") String tenantId,
                                             @Param("siteId") Long siteId,
                                             @Param("categoryId") Long categoryId,
                                             @Param("recommendationType") String recommendationType,
                                             @Param("keyword") String keyword,
                                             @Param("publishedStatus") String publishedStatus,
                                             @Param("scheduledStatus") String scheduledStatus,
                                             @Param("contentStatus") String contentStatus,
                                             @Param("now") LocalDateTime now);
}
