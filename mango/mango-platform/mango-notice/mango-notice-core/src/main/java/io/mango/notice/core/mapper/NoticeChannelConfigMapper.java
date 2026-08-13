package io.mango.notice.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import io.mango.notice.core.entity.NoticeChannelConfigEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@org.apache.ibatis.annotations.Mapper
public interface NoticeChannelConfigMapper extends BaseMapper<NoticeChannelConfigEntity> {

    @InterceptorIgnore(tenantLine = "true")
    NoticeChannelConfigEntity selectInboundConfigById(@Param("id") Long id);

    @InterceptorIgnore(tenantLine = "true")
    NoticeChannelConfigEntity selectInboundConfigByCode(@Param("configCode") String configCode);

    @InterceptorIgnore(tenantLine = "true")
    List<NoticeChannelConfigEntity> selectEnabledEmailConfigs();
}
