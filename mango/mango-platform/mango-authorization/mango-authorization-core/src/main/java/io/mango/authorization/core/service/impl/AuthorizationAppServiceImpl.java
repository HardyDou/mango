package io.mango.authorization.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.authorization.api.command.AppCommand;
import io.mango.authorization.api.vo.AppVO;
import io.mango.authorization.core.entity.AuthorizationApp;
import io.mango.authorization.core.mapper.AuthorizationAppMapper;
import io.mango.authorization.core.service.IAuthorizationAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 授权应用入口服务实现。
 */
@Service
@RequiredArgsConstructor
public class AuthorizationAppServiceImpl implements IAuthorizationAppService {

    private final AuthorizationAppMapper appMapper;

    @Override
    public List<AppVO> list() {
        LambdaQueryWrapper<AuthorizationApp> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AuthorizationApp::getStatus, 1)
                .orderByAsc(AuthorizationApp::getSort);
        return appMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    public AppVO get(Long appId) {
        AuthorizationApp app = appMapper.selectById(appId);
        return app == null ? null : toVO(app);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(AppCommand command) {
        AuthorizationApp app = toEntity(command);
        app.setCreateTime(LocalDateTime.now());
        app.setUpdateTime(LocalDateTime.now());
        appMapper.insert(app);
        return app.getAppId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean update(AppCommand command) {
        if (command == null || command.getAppId() == null) {
            return false;
        }
        AuthorizationApp existing = appMapper.selectById(command.getAppId());
        if (existing == null) {
            return false;
        }
        existing.setAppCode(command.getAppCode());
        existing.setAppName(command.getAppName());
        existing.setRealm(command.getRealm());
        existing.setActorType(command.getActorType());
        existing.setIcon(command.getIcon());
        existing.setSort(command.getSort());
        existing.setStatus(command.getStatus());
        existing.setRemark(command.getRemark());
        existing.setUpdateTime(LocalDateTime.now());
        return appMapper.updateById(existing) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean delete(Long appId) {
        return appId != null && appMapper.deleteById(appId) > 0;
    }

    private AuthorizationApp toEntity(AppCommand command) {
        AuthorizationApp app = new AuthorizationApp();
        app.setAppId(command.getAppId());
        app.setAppCode(command.getAppCode());
        app.setAppName(command.getAppName());
        app.setRealm(command.getRealm());
        app.setActorType(command.getActorType());
        app.setIcon(command.getIcon());
        app.setSort(command.getSort() == null ? 0 : command.getSort());
        app.setStatus(command.getStatus() == null ? 1 : command.getStatus());
        app.setRemark(command.getRemark());
        return app;
    }

    private AppVO toVO(AuthorizationApp app) {
        AppVO vo = new AppVO();
        vo.setAppId(app.getAppId());
        vo.setAppCode(app.getAppCode());
        vo.setAppName(app.getAppName());
        vo.setRealm(app.getRealm());
        vo.setActorType(app.getActorType());
        vo.setIcon(app.getIcon());
        vo.setSort(app.getSort());
        vo.setStatus(app.getStatus());
        vo.setRemark(app.getRemark());
        vo.setCreateTime(app.getCreateTime());
        vo.setUpdateTime(app.getUpdateTime());
        return vo;
    }
}
