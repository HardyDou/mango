package io.mango.authorization.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.mango.authorization.api.command.AppCommand;
import io.mango.authorization.api.vo.AppVO;
import io.mango.authorization.core.entity.AuthorizationApp;
import io.mango.authorization.core.mapper.AuthorizationAppMapper;
import io.mango.authorization.core.service.IAuthorizationAppService;
import io.mango.infra.persistence.starter.crud.MangoCrudServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 授权应用入口服务实现。
 */
@Service
public class AuthorizationAppServiceImpl
        extends MangoCrudServiceImpl<AuthorizationAppMapper, AuthorizationApp>
        implements IAuthorizationAppService {

    @Override
    public List<AppVO> listByQuery(Object query) {
        QueryWrapper<AuthorizationApp> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1)
                .orderByAsc("sort");
        return super.list(wrapper).stream().map(app -> (AppVO) toVO(app)).toList();
    }

    @Override
    public AppVO get(Long appId) {
        return (AppVO) detailById(appId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(AppCommand command) {
        return (Long) super.createByCommand(command);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean update(AppCommand command) {
        if (command == null || command.getAppId() == null) {
            return false;
        }
        return super.updateByCommand(command);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean delete(Long appId) {
        return appId != null && super.deleteById(appId);
    }

    @Override
    protected Class<AuthorizationApp> entityType() {
        return AuthorizationApp.class;
    }

    @Override
    protected AuthorizationApp toEntity(Object source) {
        AuthorizationApp app = super.toEntity(source);
        if (app.getSort() == null) {
            app.setSort(0);
        }
        if (app.getStatus() == null) {
            app.setStatus(1);
        }
        return app;
    }

    @Override
    protected Object toVO(AuthorizationApp app) {
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

    @Override
    protected void beforeCreate(Object command, AuthorizationApp entity) {
        LocalDateTime now = LocalDateTime.now();
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
    }

    @Override
    protected void beforeUpdate(Object command, AuthorizationApp entity) {
        entity.setUpdateTime(LocalDateTime.now());
    }
}
