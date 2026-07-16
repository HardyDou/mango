package io.mango.system.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.system.api.enums.SystemCode;
import io.mango.system.api.command.RecordLoginLogCommand;
import io.mango.system.api.command.RecordOperationLogCommand;
import io.mango.system.api.query.LoginLogPageQuery;
import io.mango.system.api.query.OperationLogPageQuery;
import io.mango.system.api.vo.LoginStatisticsVO;
import io.mango.system.api.vo.SysLoginLogVO;
import io.mango.system.api.vo.SysOperationLogVO;
import io.mango.system.core.entity.SysLoginLogEntity;
import io.mango.system.core.entity.SysOperationLogEntity;
import io.mango.system.core.mapper.SysLoginLogMapper;
import io.mango.system.core.mapper.SysOperationLogMapper;
import io.mango.system.core.service.ISysLogService;
import io.mango.system.api.spi.LoginLogRecorder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SysLogService implements ISysLogService, LoginLogRecorder {

    private static final long WEEK_START_OFFSET_DAYS = 6L;
    private static final long MONTH_START_OFFSET_DAYS = 29L;

    private final SysLoginLogMapper sysLoginLogMapper;
    private final SysOperationLogMapper sysOperationLogMapper;

    @Override
    public PageResult<SysLoginLogVO> pageLoginLogs(LoginLogPageQuery query) {
        LoginLogPageQuery resolved = query;
        if (resolved == null) {
            resolved = new LoginLogPageQuery();
        }
        IPage<SysLoginLogEntity> page = sysLoginLogMapper.selectPage(
                new Page<>(resolved.getPage(), resolved.getSize()), loginLogWrapper(resolved));
        List<SysLoginLogVO> records = page.getRecords().stream().map(this::toLoginVO).toList();
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public SysLoginLogVO getLoginLog(Long id) {
        SysLoginLogEntity entity = sysLoginLogMapper.selectOne(tenantScopedLoginWrapper()
                .eq(SysLoginLogEntity::getId, id));
        Require.notNull(entity, SystemCode.LOG_NOT_FOUND, "登录日志不存在");
        return toLoginVO(entity);
    }

    @Override
    public boolean record(RecordLoginLogCommand command) {
        Require.notNull(command, SystemCode.SYSTEM_INVALID, "登录日志不能为空");
        SysLoginLogEntity entity = new SysLoginLogEntity();
        entity.setId(command.getId());
        entity.setTenantId(command.getTenantId());
        entity.setUserId(command.getUserId());
        entity.setUsername(command.getUsername());
        entity.setLoginType(command.getLoginType());
        entity.setIp(command.getIp());
        entity.setLocation(command.getLocation());
        entity.setBrowser(command.getBrowser());
        entity.setOs(command.getOs());
        entity.setStatus(command.getStatus());
        entity.setMsg(command.getMsg());
        LocalDateTime loginTime = command.getLoginTime();
        if (loginTime == null) {
            loginTime = LocalDateTime.now();
        }
        entity.setLoginTime(loginTime);
        return sysLoginLogMapper.insert(entity) > 0;
    }

    @Override
    public Boolean cleanLoginLogs(Integer retentionDays) {
        LambdaQueryWrapper<SysLoginLogEntity> wrapper = tenantScopedLoginWrapper();
        if (retentionDays != null && retentionDays > 0) {
            wrapper.lt(SysLoginLogEntity::getLoginTime, LocalDateTime.now().minusDays(retentionDays));
        }
        sysLoginLogMapper.delete(wrapper);
        return true;
    }

    @Override
    public LoginStatisticsVO loginStatistics() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LoginStatisticsVO statistics = new LoginStatisticsVO();
        statistics.setTodayCount(countLoginLogs(todayStart, null));
        statistics.setWeekCount(countLoginLogs(todayStart.minusDays(WEEK_START_OFFSET_DAYS), null));
        statistics.setMonthCount(countLoginLogs(todayStart.minusDays(MONTH_START_OFFSET_DAYS), null));
        statistics.setTotalCount(countLoginLogs(null, null));
        statistics.setSuccessCount(countLoginLogs(null, 1));
        statistics.setFailCount(countLoginLogs(null, 0));
        return statistics;
    }

    @Override
    public PageResult<SysOperationLogVO> pageOperationLogs(OperationLogPageQuery query) {
        OperationLogPageQuery resolved = query;
        if (resolved == null) {
            resolved = new OperationLogPageQuery();
        }
        IPage<SysOperationLogEntity> page = sysOperationLogMapper.selectPage(
                new Page<>(resolved.getPage(), resolved.getSize()), operationLogWrapper(resolved));
        List<SysOperationLogVO> records = page.getRecords().stream().map(this::toOperationVO).toList();
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public SysOperationLogVO getOperationLog(Long id) {
        SysOperationLogEntity entity = sysOperationLogMapper.selectOne(tenantScopedOperationWrapper()
                .eq(SysOperationLogEntity::getId, id));
        Require.notNull(entity, SystemCode.LOG_NOT_FOUND, "操作日志不存在");
        return toOperationVO(entity);
    }

    @Override
    public boolean recordOperationLog(RecordOperationLogCommand command) {
        Require.notNull(command, SystemCode.SYSTEM_INVALID, "操作日志不能为空");
        SysOperationLogEntity entity = new SysOperationLogEntity();
        entity.setId(command.getId());
        entity.setTenantId(command.getTenantId());
        entity.setUserId(command.getUserId());
        entity.setUsername(command.getUsername());
        entity.setModule(command.getModule());
        entity.setOperation(command.getOperation());
        entity.setMethod(command.getMethod());
        entity.setHandlerMethod(command.getHandlerMethod());
        entity.setUrl(command.getUrl());
        entity.setParams(command.getParams());
        entity.setResult(command.getResult());
        entity.setStatus(command.getStatus());
        entity.setErrorMsg(command.getErrorMsg());
        entity.setDuration(command.getDuration());
        entity.setIp(command.getIp());
        entity.setLocation(command.getLocation());
        LocalDateTime operateTime = command.getOperateTime();
        if (operateTime == null) {
            operateTime = LocalDateTime.now();
        }
        entity.setOperateTime(operateTime);
        return sysOperationLogMapper.insert(entity) > 0;
    }

    @Override
    public Boolean cleanOperationLogs(Integer retentionDays) {
        LambdaQueryWrapper<SysOperationLogEntity> wrapper = tenantScopedOperationWrapper();
        if (retentionDays != null && retentionDays > 0) {
            wrapper.lt(SysOperationLogEntity::getOperateTime, LocalDateTime.now().minusDays(retentionDays));
        }
        sysOperationLogMapper.delete(wrapper);
        return true;
    }

    private long countLoginLogs(LocalDateTime since, Integer status) {
        return sysLoginLogMapper.selectCount(tenantScopedLoginWrapper()
                .ge(since != null, SysLoginLogEntity::getLoginTime, since)
                .eq(status != null, SysLoginLogEntity::getStatus, status));
    }

    private LambdaQueryWrapper<SysLoginLogEntity> loginLogWrapper(LoginLogPageQuery query) {
        String keyword = trim(query.getKeyword());
        return tenantScopedLoginWrapper()
                .and(StringUtils.hasText(keyword), nested -> nested.like(SysLoginLogEntity::getUsername, keyword)
                        .or().like(SysLoginLogEntity::getIp, keyword))
                .eq(query.getStatus() != null, SysLoginLogEntity::getStatus, query.getStatus())
                .ge(query.getStartTime() != null, SysLoginLogEntity::getLoginTime, query.getStartTime())
                .le(query.getEndTime() != null, SysLoginLogEntity::getLoginTime, query.getEndTime())
                .orderByDesc(SysLoginLogEntity::getLoginTime);
    }

    private LambdaQueryWrapper<SysOperationLogEntity> operationLogWrapper(OperationLogPageQuery query) {
        String keyword = trim(query.getKeyword());
        String username = trim(query.getUsername());
        return tenantScopedOperationWrapper()
                .and(StringUtils.hasText(keyword), nested -> nested.like(SysOperationLogEntity::getUsername, keyword)
                        .or().like(SysOperationLogEntity::getOperation, keyword)
                        .or().like(SysOperationLogEntity::getUrl, keyword))
                .like(StringUtils.hasText(username), SysOperationLogEntity::getUsername, username)
                .eq(query.getStatus() != null, SysOperationLogEntity::getStatus, query.getStatus())
                .ge(query.getStartTime() != null, SysOperationLogEntity::getOperateTime, query.getStartTime())
                .le(query.getEndTime() != null, SysOperationLogEntity::getOperateTime, query.getEndTime())
                .orderByDesc(SysOperationLogEntity::getOperateTime);
    }

    private LambdaQueryWrapper<SysLoginLogEntity> tenantScopedLoginWrapper() {
        String tenantId = MangoContextHolder.tenantId();
        return new LambdaQueryWrapper<SysLoginLogEntity>()
                .eq(!isPlatformTenant(tenantId), SysLoginLogEntity::getTenantId, tenantId);
    }

    private LambdaQueryWrapper<SysOperationLogEntity> tenantScopedOperationWrapper() {
        String tenantId = MangoContextHolder.tenantId();
        return new LambdaQueryWrapper<SysOperationLogEntity>()
                .eq(!isPlatformTenant(tenantId), SysOperationLogEntity::getTenantId, tenantId);
    }

    private boolean isPlatformTenant(String tenantId) {
        return !StringUtils.hasText(tenantId) || "default".equals(tenantId) || "1".equals(tenantId);
    }

    private SysLoginLogVO toLoginVO(SysLoginLogEntity entity) {
        SysLoginLogVO vo = new SysLoginLogVO();
        vo.setId(entity.getId());
        vo.setTenantId(entity.getTenantId());
        vo.setUserId(entity.getUserId());
        vo.setUsername(entity.getUsername());
        vo.setLoginType(entity.getLoginType());
        vo.setIp(entity.getIp());
        vo.setLocation(entity.getLocation());
        vo.setBrowser(entity.getBrowser());
        vo.setOs(entity.getOs());
        vo.setStatus(entity.getStatus());
        vo.setMsg(entity.getMsg());
        vo.setLoginTime(entity.getLoginTime());
        return vo;
    }

    private SysOperationLogVO toOperationVO(SysOperationLogEntity entity) {
        SysOperationLogVO vo = new SysOperationLogVO();
        vo.setId(entity.getId());
        vo.setTenantId(entity.getTenantId());
        vo.setUserId(entity.getUserId());
        vo.setUsername(entity.getUsername());
        vo.setModule(entity.getModule());
        vo.setOperation(entity.getOperation());
        vo.setMethod(entity.getMethod());
        vo.setHandlerMethod(entity.getHandlerMethod());
        vo.setUrl(entity.getUrl());
        vo.setParams(entity.getParams());
        vo.setResult(entity.getResult());
        vo.setStatus(entity.getStatus());
        vo.setErrorMsg(entity.getErrorMsg());
        vo.setDuration(entity.getDuration());
        vo.setIp(entity.getIp());
        vo.setLocation(entity.getLocation());
        vo.setOperateTime(entity.getOperateTime());
        return vo;
    }

    private String trim(String value) {
        if (StringUtils.hasText(value)) {
            return value.trim();
        }
        return null;
    }
}
