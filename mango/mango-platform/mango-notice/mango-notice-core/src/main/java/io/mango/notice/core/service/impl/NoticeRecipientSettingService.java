package io.mango.notice.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.common.result.Require;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.notice.api.command.SaveNoticeReceivePreferenceCommand;
import io.mango.notice.api.command.SaveNoticeRecipientAccountCommand;
import io.mango.notice.api.command.SaveNoticeSettingsCommand;
import io.mango.notice.api.enums.NoticeCode;
import io.mango.notice.api.enums.NoticeReceivePreferenceScopeType;
import io.mango.notice.api.enums.NoticeRecipientAccountStatus;
import io.mango.notice.api.enums.NoticeRecipientAccountType;
import io.mango.notice.api.query.NoticeReceivePreferenceQuery;
import io.mango.notice.api.query.NoticeRecipientAccountQuery;
import io.mango.notice.api.vo.NoticeReceivePreferenceVO;
import io.mango.notice.api.vo.NoticeRecipientAccountVO;
import io.mango.notice.api.vo.NoticeSettingsVO;
import io.mango.notice.core.convert.NoticeReceivePreferenceConvert;
import io.mango.notice.core.convert.NoticeRecipientAccountConvert;
import io.mango.notice.core.entity.NoticeReceivePreferenceEntity;
import io.mango.notice.core.entity.NoticeRecipientAccountEntity;
import io.mango.notice.core.entity.NoticeSettingEntity;
import io.mango.notice.core.mapper.NoticeReceivePreferenceMapper;
import io.mango.notice.core.mapper.NoticeRecipientAccountMapper;
import io.mango.notice.core.mapper.NoticeSettingMapper;
import io.mango.notice.core.service.INoticeRecipientSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NoticeRecipientSettingService implements INoticeRecipientSettingService {

    private final NoticeRecipientAccountMapper recipientAccountMapper;
    private final NoticeReceivePreferenceMapper receivePreferenceMapper;
    private final NoticeSettingMapper settingMapper;

    @Override
    public NoticeSettingsVO getSettings() {
        NoticeSettingsVO settings = defaultSettings();
        settingMapper.selectList(new LambdaQueryWrapper<NoticeSettingEntity>())
                .forEach(setting -> applySetting(settings, setting.getSettingKey(), setting.getSettingValue()));
        return settings;
    }

    @Override
    public boolean saveSettings(SaveNoticeSettingsCommand command) {
        Require.notNull(command, NoticeCode.NOTICE_BUSINESS_ERROR, "通知设置保存命令不能为空");
        NoticeSettingsVO defaults = defaultSettings();
        upsertSetting("soundEnabled", String.valueOf(command.getSoundEnabled() == null
                ? defaults.getSoundEnabled() : command.getSoundEnabled()));
        upsertSetting("desktopEnabled", String.valueOf(command.getDesktopEnabled() == null
                ? defaults.getDesktopEnabled() : command.getDesktopEnabled()));
        upsertSetting("maxRetry", String.valueOf(command.getMaxRetry() == null
                ? defaults.getMaxRetry() : command.getMaxRetry()));
        upsertSetting("retentionDays", String.valueOf(command.getRetentionDays() == null
                ? defaults.getRetentionDays() : command.getRetentionDays()));
        return true;
    }

    @Override
    public List<NoticeRecipientAccountVO> listRecipientAccounts(NoticeRecipientAccountQuery query) {
        Long userId = resolveTargetUserId(currentUserId(), query == null ? null : query.getUserId());
        LambdaQueryWrapper<NoticeRecipientAccountEntity> wrapper =
                new LambdaQueryWrapper<NoticeRecipientAccountEntity>()
                        .eq(NoticeRecipientAccountEntity::getUserId, userId)
                        .eq(NoticeRecipientAccountEntity::getEnabled, true);
        if (query != null && query.getAccountType() != null) {
            wrapper.eq(NoticeRecipientAccountEntity::getAccountType, query.getAccountType());
        }
        wrapper.orderByDesc(NoticeRecipientAccountEntity::getDefaultAccount)
                .orderByDesc(NoticeRecipientAccountEntity::getUpdatedAt);
        return recipientAccountMapper.selectList(wrapper).stream()
                .map(NoticeRecipientAccountConvert::toVO)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NoticeRecipientAccountVO saveRecipientAccount(SaveNoticeRecipientAccountCommand command) {
        Require.notNull(command, NoticeCode.NOTICE_BUSINESS_ERROR, "接收账户不能为空");
        Long userId = resolveTargetUserId(currentUserId(), command.getUserId());
        NoticeRecipientAccountEntity entity = command.getId() == null
                ? new NoticeRecipientAccountEntity()
                : recipientAccountMapper.selectById(command.getId());
        Require.notNull(entity, NoticeCode.NOTICE_BUSINESS_ERROR, "接收账户不存在");
        entity.setUserId(userId);
        entity.setAccountType(command.getAccountType());
        entity.setAccountValue(command.getAccountValue());
        entity.setDisplayName(command.getDisplayName());
        entity.setVerifiedStatus(command.getVerifiedStatus() == null
                ? NoticeRecipientAccountStatus.VERIFIED : command.getVerifiedStatus());
        entity.setDefaultAccount(Boolean.TRUE.equals(command.getDefaultAccount()));
        entity.setEnabled(true);
        if (Boolean.TRUE.equals(entity.getDefaultAccount())) {
            clearDefaultAccount(userId, entity.getAccountType());
        }
        if (entity.getId() == null) {
            recipientAccountMapper.insert(entity);
        } else {
            recipientAccountMapper.updateById(entity);
        }
        return NoticeRecipientAccountConvert.toVO(entity);
    }

    @Override
    public boolean disableRecipientAccount(Long id, Long userId) {
        Require.notNull(id, NoticeCode.NOTICE_BUSINESS_ERROR, "接收账户 ID 不能为空");
        Long targetUserId = resolveTargetUserId(currentUserId(), userId);
        NoticeRecipientAccountEntity entity = new NoticeRecipientAccountEntity();
        entity.setId(id);
        entity.setEnabled(false);
        entity.setVerifiedStatus(NoticeRecipientAccountStatus.DISABLED);
        return recipientAccountMapper.update(entity, new LambdaQueryWrapper<NoticeRecipientAccountEntity>()
                .eq(NoticeRecipientAccountEntity::getId, id)
                .eq(NoticeRecipientAccountEntity::getUserId, targetUserId)) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean setDefaultRecipientAccount(Long id, Long userId) {
        Require.notNull(id, NoticeCode.NOTICE_BUSINESS_ERROR, "接收账户 ID 不能为空");
        Long targetUserId = resolveTargetUserId(currentUserId(), userId);
        NoticeRecipientAccountEntity account = recipientAccountMapper.selectOne(
                new LambdaQueryWrapper<NoticeRecipientAccountEntity>()
                        .eq(NoticeRecipientAccountEntity::getId, id)
                        .eq(NoticeRecipientAccountEntity::getUserId, targetUserId));
        Require.notNull(account, NoticeCode.NOTICE_BUSINESS_ERROR, "接收账户不存在");
        clearDefaultAccount(targetUserId, account.getAccountType());
        account.setDefaultAccount(true);
        return recipientAccountMapper.updateById(account) > 0;
    }

    @Override
    public List<NoticeReceivePreferenceVO> listReceivePreferences(NoticeReceivePreferenceQuery query) {
        Long userId = resolveTargetUserId(currentUserId(), query == null ? null : query.getUserId());
        LambdaQueryWrapper<NoticeReceivePreferenceEntity> wrapper =
                new LambdaQueryWrapper<NoticeReceivePreferenceEntity>()
                        .eq(NoticeReceivePreferenceEntity::getUserId, userId);
        if (query != null && query.getScopeType() != null) {
            wrapper.eq(NoticeReceivePreferenceEntity::getScopeType, query.getScopeType());
        }
        if (query != null && StringUtils.hasText(query.getScopeValue())) {
            wrapper.eq(NoticeReceivePreferenceEntity::getScopeValue, query.getScopeValue());
        }
        wrapper.orderByAsc(NoticeReceivePreferenceEntity::getScopeType)
                .orderByAsc(NoticeReceivePreferenceEntity::getScopeValue)
                .orderByAsc(NoticeReceivePreferenceEntity::getChannelType);
        return receivePreferenceMapper.selectList(wrapper).stream()
                .map(NoticeReceivePreferenceConvert::toVO)
                .toList();
    }

    @Override
    public NoticeReceivePreferenceVO saveReceivePreference(SaveNoticeReceivePreferenceCommand command) {
        Require.notNull(command, NoticeCode.NOTICE_BUSINESS_ERROR, "接收偏好不能为空");
        Long userId = resolveTargetUserId(currentUserId(), command.getUserId());
        String scopeValue = normalizeScopeValue(command.getScopeValue());
        NoticeReceivePreferenceEntity entity = findPreference(
                userId, command.getScopeType(), scopeValue, command.getChannelType());
        if (entity == null) {
            entity = new NoticeReceivePreferenceEntity();
            entity.setUserId(userId);
            entity.setScopeType(command.getScopeType());
            entity.setScopeValue(scopeValue);
            entity.setChannelType(command.getChannelType());
        }
        entity.setEnabled(command.getEnabled());
        entity.setAccountId(command.getAccountId());
        if (entity.getId() == null) {
            receivePreferenceMapper.insert(entity);
        } else {
            receivePreferenceMapper.updateById(entity);
        }
        return NoticeReceivePreferenceConvert.toVO(entity);
    }

    private NoticeReceivePreferenceEntity findPreference(
            Long userId,
            NoticeReceivePreferenceScopeType scopeType,
            String scopeValue,
            io.mango.notice.api.enums.NoticeChannelType channelType) {
        if (userId == null || scopeType == null) {
            return null;
        }
        LambdaQueryWrapper<NoticeReceivePreferenceEntity> wrapper =
                new LambdaQueryWrapper<NoticeReceivePreferenceEntity>()
                .eq(NoticeReceivePreferenceEntity::getUserId, userId)
                .eq(NoticeReceivePreferenceEntity::getScopeType, scopeType)
                .eq(NoticeReceivePreferenceEntity::getScopeValue, scopeValue);
        if (channelType == null) {
            wrapper.isNull(NoticeReceivePreferenceEntity::getChannelType);
        } else {
            wrapper.eq(NoticeReceivePreferenceEntity::getChannelType, channelType);
        }
        return receivePreferenceMapper.selectOne(wrapper);
    }

    private void clearDefaultAccount(Long userId, NoticeRecipientAccountType accountType) {
        NoticeRecipientAccountEntity update = new NoticeRecipientAccountEntity();
        update.setDefaultAccount(false);
        recipientAccountMapper.update(update, new LambdaQueryWrapper<NoticeRecipientAccountEntity>()
                .eq(NoticeRecipientAccountEntity::getUserId, userId)
                .eq(NoticeRecipientAccountEntity::getAccountType, accountType)
                .eq(NoticeRecipientAccountEntity::getDefaultAccount, true));
    }

    private void upsertSetting(String key, String value) {
        NoticeSettingEntity existing = settingMapper.selectOne(new LambdaQueryWrapper<NoticeSettingEntity>()
                .eq(NoticeSettingEntity::getSettingKey, key)
                .last("LIMIT 1"));
        NoticeSettingEntity entity = existing == null ? new NoticeSettingEntity() : existing;
        entity.setSettingKey(key);
        entity.setSettingValue(value);
        if (entity.getId() == null) {
            settingMapper.insert(entity);
        } else {
            settingMapper.updateById(entity);
        }
    }

    private NoticeSettingsVO defaultSettings() {
        NoticeSettingsVO settings = new NoticeSettingsVO();
        settings.setSoundEnabled(true);
        settings.setDesktopEnabled(true);
        settings.setMaxRetry(3);
        settings.setRetentionDays(180);
        return settings;
    }

    private void applySetting(NoticeSettingsVO settings, String key, String value) {
        if (!StringUtils.hasText(key) || !StringUtils.hasText(value)) {
            return;
        }
        switch (key) {
            case "soundEnabled" -> settings.setSoundEnabled(Boolean.parseBoolean(value));
            case "desktopEnabled" -> settings.setDesktopEnabled(Boolean.parseBoolean(value));
            case "maxRetry" -> settings.setMaxRetry(parseInteger(value, settings.getMaxRetry()));
            case "retentionDays" -> settings.setRetentionDays(parseInteger(value, settings.getRetentionDays()));
            default -> {
            }
        }
    }

    private Integer parseInteger(String value, Integer defaultValue) {
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private Long resolveTargetUserId(Long currentUserId, Long requestedUserId) {
        return requestedUserId == null ? currentUserId : requestedUserId;
    }

    private Long currentUserId() {
        Long userId = MangoContextHolder.userId();
        Require.notNull(userId, NoticeCode.NOTICE_BUSINESS_ERROR, "当前用户不能为空");
        return userId;
    }

    private String normalizeScopeValue(String scopeValue) {
        return StringUtils.hasText(scopeValue) ? scopeValue : "";
    }
}
