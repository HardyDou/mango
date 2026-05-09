package io.mango.system.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.infra.context.core.MangoContextHolder;
import io.mango.system.api.po.SysLoginLogPo;
import io.mango.system.api.po.SysOperationLogPo;
import io.mango.system.api.query.LoginLogPageQuery;
import io.mango.system.api.query.OperationLogPageQuery;
import io.mango.system.core.entity.SysLoginLog;
import io.mango.system.core.entity.SysOperationLog;
import io.mango.system.core.mapper.SysLoginLogMapper;
import io.mango.system.core.mapper.SysOperationLogMapper;
import io.mango.system.core.service.ISysLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysLogServiceImpl implements ISysLogService {

    private final SysLoginLogMapper sysLoginLogMapper;
    private final SysOperationLogMapper sysOperationLogMapper;

    @Override
    public R<List<SysLoginLogPo>> listLoginLogs() {
        LambdaQueryWrapper<SysLoginLog> wrapper = loginLogWrapper(new LoginLogPageQuery());
        List<SysLoginLog> list = sysLoginLogMapper.selectList(wrapper);
        List<SysLoginLogPo> poList = list.stream().map(this::convertToLoginPo).collect(Collectors.toList());
        return R.ok(poList);
    }

    @Override
    public R<PageResult<SysLoginLogPo>> pageLoginLogs(LoginLogPageQuery query) {
        LoginLogPageQuery resolvedQuery = query == null ? new LoginLogPageQuery() : query;
        IPage<SysLoginLog> page = sysLoginLogMapper.selectPage(
                new Page<>(resolvedQuery.getPage(), resolvedQuery.getSize()),
                loginLogWrapper(resolvedQuery));
        List<SysLoginLogPo> records = page.getRecords().stream()
                .map(this::convertToLoginPo)
                .collect(Collectors.toList());
        return R.ok(PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize()));
    }

    @Override
    public R<SysLoginLogPo> getLoginLog(Long id) {
        SysLoginLog entity = selectTenantVisibleLoginLog(id);
        if (entity == null) {
            return R.fail("登录日志不存在");
        }
        return R.ok(convertToLoginPo(entity));
    }

    @Override
    public R<Boolean> recordLoginLog(SysLoginLogPo log) {
        if (log == null) {
            return R.fail("登录日志不能为空");
        }
        SysLoginLog entity = convertToLoginEntity(log);
        if (entity.getLoginTime() == null) {
            entity.setLoginTime(LocalDateTime.now());
        }
        sysLoginLogMapper.insert(entity);
        return R.ok(true);
    }

    @Override
    public R<Boolean> cleanLoginLogs(Integer retentionDays) {
        LambdaQueryWrapper<SysLoginLog> wrapper = tenantScopedLoginWrapper();
        if (retentionDays != null && retentionDays > 0) {
            wrapper.lt(SysLoginLog::getLoginTime, LocalDateTime.now().minusDays(retentionDays));
        }
        sysLoginLogMapper.delete(wrapper);
        return R.ok(true);
    }

    @Override
    public R<Map<String, Object>> loginStatistics() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime weekStart = todayStart.minusDays(6);
        LocalDateTime monthStart = todayStart.minusDays(29);
        Map<String, Object> stats = new HashMap<>();
        stats.put("todayCount", sysLoginLogMapper.selectCount(tenantScopedLoginWrapper()
                .ge(SysLoginLog::getLoginTime, todayStart)));
        stats.put("weekCount", sysLoginLogMapper.selectCount(tenantScopedLoginWrapper()
                .ge(SysLoginLog::getLoginTime, weekStart)));
        stats.put("monthCount", sysLoginLogMapper.selectCount(tenantScopedLoginWrapper()
                .ge(SysLoginLog::getLoginTime, monthStart)));
        stats.put("totalCount", sysLoginLogMapper.selectCount(tenantScopedLoginWrapper()));
        stats.put("successCount", sysLoginLogMapper.selectCount(tenantScopedLoginWrapper()
                .eq(SysLoginLog::getStatus, 1)));
        stats.put("failCount", sysLoginLogMapper.selectCount(tenantScopedLoginWrapper()
                .eq(SysLoginLog::getStatus, 0)));
        return R.ok(stats);
    }

    @Override
    public R<List<SysOperationLogPo>> listOperationLogs() {
        LambdaQueryWrapper<SysOperationLog> wrapper = operationLogWrapper(new OperationLogPageQuery());
        List<SysOperationLog> list = sysOperationLogMapper.selectList(wrapper);
        List<SysOperationLogPo> poList = list.stream().map(this::convertToOperationPo).collect(Collectors.toList());
        return R.ok(poList);
    }

    @Override
    public R<PageResult<SysOperationLogPo>> pageOperationLogs(OperationLogPageQuery query) {
        OperationLogPageQuery resolvedQuery = query == null ? new OperationLogPageQuery() : query;
        IPage<SysOperationLog> page = sysOperationLogMapper.selectPage(
                new Page<>(resolvedQuery.getPage(), resolvedQuery.getSize()),
                operationLogWrapper(resolvedQuery));
        List<SysOperationLogPo> records = page.getRecords().stream()
                .map(this::convertToOperationPo)
                .collect(Collectors.toList());
        return R.ok(PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize()));
    }

    @Override
    public R<SysOperationLogPo> getOperationLog(Long id) {
        SysOperationLog entity = selectTenantVisibleOperationLog(id);
        if (entity == null) {
            return R.fail("操作日志不存在");
        }
        return R.ok(convertToOperationPo(entity));
    }

    @Override
    public R<Boolean> recordOperationLog(SysOperationLogPo log) {
        if (log == null) {
            return R.fail("操作日志不能为空");
        }
        SysOperationLog entity = convertToOperationEntity(log);
        if (entity.getOperateTime() == null) {
            entity.setOperateTime(LocalDateTime.now());
        }
        sysOperationLogMapper.insert(entity);
        return R.ok(true);
    }

    @Override
    public R<Boolean> cleanOperationLogs(Integer retentionDays) {
        LambdaQueryWrapper<SysOperationLog> wrapper = tenantScopedOperationWrapper();
        if (retentionDays != null && retentionDays > 0) {
            wrapper.lt(SysOperationLog::getOperateTime, LocalDateTime.now().minusDays(retentionDays));
        }
        sysOperationLogMapper.delete(wrapper);
        return R.ok(true);
    }

    private LambdaQueryWrapper<SysLoginLog> loginLogWrapper(LoginLogPageQuery query) {
        LambdaQueryWrapper<SysLoginLog> wrapper = tenantScopedLoginWrapper();
        if (query != null) {
            String keyword = trim(query.getKeyword());
            wrapper.and(StringUtils.hasText(keyword), nested -> nested
                    .like(SysLoginLog::getUsername, keyword)
                    .or()
                    .like(SysLoginLog::getIp, keyword));
            wrapper.eq(query.getStatus() != null, SysLoginLog::getStatus, query.getStatus());
            wrapper.ge(query.getStartTime() != null, SysLoginLog::getLoginTime, query.getStartTime());
            wrapper.le(query.getEndTime() != null, SysLoginLog::getLoginTime, query.getEndTime());
        }
        wrapper.orderByDesc(SysLoginLog::getLoginTime);
        return wrapper;
    }

    private LambdaQueryWrapper<SysOperationLog> operationLogWrapper(OperationLogPageQuery query) {
        LambdaQueryWrapper<SysOperationLog> wrapper = tenantScopedOperationWrapper();
        if (query != null) {
            String keyword = trim(query.getKeyword());
            String username = trim(query.getUsername());
            wrapper.and(StringUtils.hasText(keyword), nested -> nested
                    .like(SysOperationLog::getUsername, keyword)
                    .or()
                    .like(SysOperationLog::getOperation, keyword)
                    .or()
                    .like(SysOperationLog::getUrl, keyword));
            wrapper.like(StringUtils.hasText(username), SysOperationLog::getUsername, username);
            wrapper.eq(query.getStatus() != null, SysOperationLog::getStatus, query.getStatus());
            wrapper.ge(query.getStartTime() != null, SysOperationLog::getOperateTime, query.getStartTime());
            wrapper.le(query.getEndTime() != null, SysOperationLog::getOperateTime, query.getEndTime());
        }
        wrapper.orderByDesc(SysOperationLog::getOperateTime);
        return wrapper;
    }

    private LambdaQueryWrapper<SysLoginLog> tenantScopedLoginWrapper() {
        LambdaQueryWrapper<SysLoginLog> wrapper = new LambdaQueryWrapper<>();
        Long tenantId = currentTenantId();
        wrapper.eq(tenantId != null && tenantId > 1L, SysLoginLog::getTenantId, tenantId);
        return wrapper;
    }

    private LambdaQueryWrapper<SysOperationLog> tenantScopedOperationWrapper() {
        LambdaQueryWrapper<SysOperationLog> wrapper = new LambdaQueryWrapper<>();
        Long tenantId = currentTenantId();
        wrapper.eq(tenantId != null && tenantId > 1L, SysOperationLog::getTenantId, tenantId);
        return wrapper;
    }

    private SysLoginLog selectTenantVisibleLoginLog(Long id) {
        if (id == null) {
            return null;
        }
        LambdaQueryWrapper<SysLoginLog> wrapper = tenantScopedLoginWrapper().eq(SysLoginLog::getId, id);
        return sysLoginLogMapper.selectOne(wrapper);
    }

    private SysOperationLog selectTenantVisibleOperationLog(Long id) {
        if (id == null) {
            return null;
        }
        LambdaQueryWrapper<SysOperationLog> wrapper = tenantScopedOperationWrapper().eq(SysOperationLog::getId, id);
        return sysOperationLogMapper.selectOne(wrapper);
    }

    private SysLoginLogPo convertToLoginPo(SysLoginLog entity) {
        SysLoginLogPo po = new SysLoginLogPo();
        po.setId(entity.getId());
        po.setTenantId(entity.getTenantId());
        po.setUserId(entity.getUserId());
        po.setUsername(entity.getUsername());
        po.setLoginType(entity.getLoginType());
        po.setIp(entity.getIp());
        po.setLocation(entity.getLocation());
        po.setBrowser(entity.getBrowser());
        po.setOs(entity.getOs());
        po.setStatus(entity.getStatus());
        po.setMsg(entity.getMsg());
        po.setLoginTime(entity.getLoginTime());
        return po;
    }

    private SysLoginLog convertToLoginEntity(SysLoginLogPo po) {
        SysLoginLog entity = new SysLoginLog();
        entity.setId(po.getId());
        entity.setTenantId(po.getTenantId());
        entity.setUserId(po.getUserId());
        entity.setUsername(po.getUsername());
        entity.setLoginType(po.getLoginType());
        entity.setIp(po.getIp());
        entity.setLocation(po.getLocation());
        entity.setBrowser(po.getBrowser());
        entity.setOs(po.getOs());
        entity.setStatus(po.getStatus());
        entity.setMsg(po.getMsg());
        entity.setLoginTime(po.getLoginTime());
        return entity;
    }

    private SysOperationLogPo convertToOperationPo(SysOperationLog entity) {
        SysOperationLogPo po = new SysOperationLogPo();
        po.setId(entity.getId());
        po.setTenantId(entity.getTenantId());
        po.setUserId(entity.getUserId());
        po.setUsername(entity.getUsername());
        po.setModule(entity.getModule());
        po.setOperation(entity.getOperation());
        po.setMethod(entity.getMethod());
        po.setHandlerMethod(entity.getHandlerMethod());
        po.setUrl(entity.getUrl());
        po.setParams(entity.getParams());
        po.setResult(entity.getResult());
        po.setStatus(entity.getStatus());
        po.setErrorMsg(entity.getErrorMsg());
        po.setDuration(entity.getDuration());
        po.setIp(entity.getIp());
        po.setLocation(entity.getLocation());
        po.setOperateTime(entity.getOperateTime());
        return po;
    }

    private SysOperationLog convertToOperationEntity(SysOperationLogPo po) {
        SysOperationLog entity = new SysOperationLog();
        entity.setId(po.getId());
        entity.setTenantId(po.getTenantId());
        entity.setUserId(po.getUserId());
        entity.setUsername(po.getUsername());
        entity.setModule(po.getModule());
        entity.setOperation(po.getOperation());
        entity.setMethod(po.getMethod());
        entity.setHandlerMethod(po.getHandlerMethod());
        entity.setUrl(po.getUrl());
        entity.setParams(po.getParams());
        entity.setResult(po.getResult());
        entity.setStatus(po.getStatus());
        entity.setErrorMsg(po.getErrorMsg());
        entity.setDuration(po.getDuration());
        entity.setIp(po.getIp());
        entity.setLocation(po.getLocation());
        entity.setOperateTime(po.getOperateTime());
        return entity;
    }

    private Long currentTenantId() {
        String tenantId = MangoContextHolder.tenantId();
        if (!StringUtils.hasText(tenantId)) {
            return null;
        }
        try {
            return Long.valueOf(tenantId);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
