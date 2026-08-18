package io.mango.notice.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import io.mango.notice.core.entity.NoticeAuditLogEntity;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NoticeAuditLogMapper extends BaseMapper<NoticeAuditLogEntity> {}
