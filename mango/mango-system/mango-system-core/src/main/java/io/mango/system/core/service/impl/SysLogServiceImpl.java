package io.mango.system.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.mango.common.result.R;
import io.mango.system.api.po.SysLoginLogPo;
import io.mango.system.api.po.SysOperationLogPo;
import io.mango.system.core.entity.SysLoginLog;
import io.mango.system.core.entity.SysOperationLog;
import io.mango.system.core.mapper.SysLoginLogMapper;
import io.mango.system.core.mapper.SysOperationLogMapper;
import io.mango.system.core.service.ISysLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
        LambdaQueryWrapper<SysLoginLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(SysLoginLog::getLoginTime);
        List<SysLoginLog> list = sysLoginLogMapper.selectList(wrapper);
        List<SysLoginLogPo> poList = list.stream().map(this::convertToLoginPo).collect(Collectors.toList());
        return R.ok(poList);
    }

    @Override
    public R<SysLoginLogPo> getLoginLog(Long id) {
        SysLoginLog entity = sysLoginLogMapper.selectById(id);
        if (entity == null) {
            return R.fail("登录日志不存在");
        }
        return R.ok(convertToLoginPo(entity));
    }

    @Override
    public R<Boolean> cleanLoginLogs() {
        return R.ok(true);
    }

    @Override
    public R<Map<String, Object>> loginStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("todayCount", 0);
        stats.put("weekCount", 0);
        stats.put("monthCount", 0);
        stats.put("totalCount", sysLoginLogMapper.selectCount(null));
        return R.ok(stats);
    }

    @Override
    public R<List<SysOperationLogPo>> listOperationLogs() {
        LambdaQueryWrapper<SysOperationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(SysOperationLog::getOperateTime);
        List<SysOperationLog> list = sysOperationLogMapper.selectList(wrapper);
        List<SysOperationLogPo> poList = list.stream().map(this::convertToOperationPo).collect(Collectors.toList());
        return R.ok(poList);
    }

    @Override
    public R<SysOperationLogPo> getOperationLog(Long id) {
        SysOperationLog entity = sysOperationLogMapper.selectById(id);
        if (entity == null) {
            return R.fail("操作日志不存在");
        }
        return R.ok(convertToOperationPo(entity));
    }

    @Override
    public R<Boolean> cleanOperationLogs() {
        return R.ok(true);
    }

    private SysLoginLogPo convertToLoginPo(SysLoginLog entity) {
        SysLoginLogPo po = new SysLoginLogPo();
        po.setId(entity.getId());
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

    private SysOperationLogPo convertToOperationPo(SysOperationLog entity) {
        SysOperationLogPo po = new SysOperationLogPo();
        po.setId(entity.getId());
        po.setUserId(entity.getUserId());
        po.setUsername(entity.getUsername());
        po.setModule(entity.getModule());
        po.setOperation(entity.getOperation());
        po.setMethod(entity.getMethod());
        po.setUrl(entity.getUrl());
        po.setParams(entity.getParams());
        po.setResult(entity.getResult());
        po.setStatus(entity.getStatus());
        po.setErrorMsg(entity.getErrorMsg());
        po.setDuration(entity.getDuration());
        po.setIp(entity.getIp());
        po.setOperateTime(entity.getOperateTime());
        return po;
    }
}
