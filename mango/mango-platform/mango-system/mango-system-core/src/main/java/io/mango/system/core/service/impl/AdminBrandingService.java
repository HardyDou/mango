package io.mango.system.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.common.result.Require;
import io.mango.system.api.command.SaveAdminBrandingCommand;
import io.mango.system.api.enums.ConfigOptionSourceEnum;
import io.mango.system.api.enums.ConfigTypeEnum;
import io.mango.system.api.enums.ConfigValueTypeEnum;
import io.mango.system.api.enums.SystemCode;
import io.mango.system.api.vo.AdminBrandingVO;
import io.mango.system.core.entity.SysConfigEntity;
import io.mango.system.core.mapper.SysConfigMapper;
import io.mango.system.core.service.IAdminBrandingService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminBrandingService implements IAdminBrandingService {

    private static final int ENABLED = 1;
    private static final String DOMAIN_CODE = "COMMON";
    private static final String GROUP_CODE = "admin-branding";
    private static final String GROUP_NAME = "后台品牌配置";
    private static final String FILE_TOKEN_PREFIX = "mango-file:";

    private final SysConfigMapper sysConfigMapper;

    @Override
    public AdminBrandingVO get() {
        Map<String, SysConfigEntity> configMap = loadConfigMap();
        AdminBrandingVO vo = new AdminBrandingVO();
        Arrays.stream(Field.values()).forEach(field -> field.applyTo(vo, resolveValue(configMap.get(field.key), field)));
        return vo;
    }

    @Override
    public Boolean save(SaveAdminBrandingCommand command) {
        Require.notNull(command, SystemCode.SYSTEM_INVALID, "后台品牌配置不能为空");
        validateCommand(command);
        Map<String, SysConfigEntity> configMap = loadConfigMap();
        Arrays.stream(Field.values()).forEach(field -> saveField(field, field.readFrom(command), configMap.get(field.key)));
        return true;
    }

    private void validateCommand(SaveAdminBrandingCommand command) {
        Arrays.stream(Field.values()).forEach(field -> normalizeFieldValue(field, field.readFrom(command)));
    }

    private Map<String, SysConfigEntity> loadConfigMap() {
        List<String> keys = Arrays.stream(Field.values()).map(Field::getKey).toList();
        return sysConfigMapper.selectList(new LambdaQueryWrapper<SysConfigEntity>().in(SysConfigEntity::getConfigKey, keys))
                .stream()
                .collect(Collectors.toMap(SysConfigEntity::getConfigKey, Function.identity(), (left, right) -> left, LinkedHashMap::new));
    }

    private String resolveValue(SysConfigEntity config, Field field) {
        if (config == null || Integer.valueOf(0).equals(config.getStatus()) || !StringUtils.hasText(config.getConfigValue())) {
            return field.defaultValue;
        }
        return config.getConfigValue();
    }

    private void saveField(Field field, String value, SysConfigEntity existing) {
        if (existing == null) {
            SysConfigEntity entity = new SysConfigEntity();
            entity.setConfigKey(field.key);
            entity.setConfigName(field.label);
            entity.setType(ConfigTypeEnum.SYSTEM);
            entity.setDomainCode(DOMAIN_CODE);
            entity.setValueType(field.valueType);
            entity.setGroupCode(GROUP_CODE);
            entity.setGroupName(GROUP_NAME);
            entity.setDefaultValue(field.defaultValue);
            entity.setOptions("");
            entity.setOptionSource(ConfigOptionSourceEnum.CUSTOM);
            entity.setEditable(true);
            entity.setSort(field.sort);
            entity.setStatus(ENABLED);
            entity.setRemark(field.label);
            entity.setConfigValue(normalizeFieldValue(field, value));
            sysConfigMapper.insert(entity);
            return;
        }

        SysConfigEntity entity = new SysConfigEntity();
        entity.setId(existing.getId());
        entity.setConfigValue(normalizeFieldValue(field, value));
        entity.setConfigName(field.label);
        entity.setType(ConfigTypeEnum.SYSTEM);
        entity.setDomainCode(DOMAIN_CODE);
        entity.setValueType(field.valueType);
        entity.setGroupCode(GROUP_CODE);
        entity.setGroupName(GROUP_NAME);
        entity.setDefaultValue(field.defaultValue);
        entity.setOptionSource(ConfigOptionSourceEnum.CUSTOM);
        entity.setEditable(true);
        entity.setSort(field.sort);
        entity.setStatus(ENABLED);
        entity.setRemark(field.label);
        sysConfigMapper.updateById(entity);
    }

    private String normalizeValue(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    private String normalizeFieldValue(Field field, String value) {
        String normalized = normalizeValue(value);
        if (field.fileField && StringUtils.hasText(normalized)) {
            // 兼容历史 mango-file:{id} token，新的保存值统一落库为文件中心 ID。
            String fileId = normalized;
            if (normalized.startsWith(FILE_TOKEN_PREFIX)) {
                fileId = normalized.substring(FILE_TOKEN_PREFIX.length()).trim();
            }
            Require.isTrue(fileId.matches("[1-9]\\d*"), SystemCode.SYSTEM_INVALID,
                    field.label + "只能保存文件中心 ID");
            return fileId;
        }
        return normalized;
    }

    @Getter
    @RequiredArgsConstructor
    private enum Field {
        ENABLED("admin.branding.enabled", "启用状态", "true", 5,
                ConfigValueTypeEnum.BOOLEAN,
                command -> String.valueOf(!Boolean.FALSE.equals(command.getEnabled())),
                (vo, value) -> vo.setEnabled(Boolean.parseBoolean(value))),
        TITLE("admin.branding.title", "后台名称", "Mango Admin", 10,
                SaveAdminBrandingCommand::getTitle, AdminBrandingVO::setTitle),
        SHORT_TITLE("admin.branding.shortTitle", "后台简称", "Mango", 20,
                SaveAdminBrandingCommand::getShortTitle, AdminBrandingVO::setShortTitle),
        SUBTITLE("admin.branding.subtitle", "后台副标题", "企业级管理平台", 30,
                SaveAdminBrandingCommand::getSubtitle, AdminBrandingVO::setSubtitle),
        LOGIN_TITLE("admin.branding.loginTitle", "登录页标题", "Mango Admin", 40,
                SaveAdminBrandingCommand::getLoginTitle, AdminBrandingVO::setLoginTitle),
        LOGIN_SUBTITLE("admin.branding.loginSubtitle", "登录页副标题", "企业级管理平台", 50,
                SaveAdminBrandingCommand::getLoginSubtitle, AdminBrandingVO::setLoginSubtitle),
        LOGO_FILE("admin.branding.logoFile", "后台 Logo 文件", "", 60, true,
                SaveAdminBrandingCommand::getLogoFile, AdminBrandingVO::setLogoFile),
        FAVICON_FILE("admin.branding.faviconFile", "浏览器 favicon 文件", "", 70, true,
                SaveAdminBrandingCommand::getFaviconFile, AdminBrandingVO::setFaviconFile),
        LOGIN_IMAGE_FILE("admin.branding.loginImageFile", "登录页图片文件", "", 80, true,
                SaveAdminBrandingCommand::getLoginImageFile, AdminBrandingVO::setLoginImageFile),
        FOOTER_COPYRIGHT("admin.branding.footerCopyright", "页脚版权", "© Mango", 90,
                SaveAdminBrandingCommand::getFooterCopyright, AdminBrandingVO::setFooterCopyright),
        ICP("admin.branding.icp", "备案号", "", 100,
                SaveAdminBrandingCommand::getIcp, AdminBrandingVO::setIcp),
        CONTACT("admin.branding.contact", "联系方式", "", 110,
                SaveAdminBrandingCommand::getContact, AdminBrandingVO::setContact);

        private final String key;
        private final String label;
        private final String defaultValue;
        private final int sort;
        private final ConfigValueTypeEnum valueType;
        private final boolean fileField;
        private final Function<SaveAdminBrandingCommand, String> commandReader;
        private final BiConsumer<AdminBrandingVO, String> voWriter;

        Field(String key, String label, String defaultValue, int sort,
              Function<SaveAdminBrandingCommand, String> commandReader,
              BiConsumer<AdminBrandingVO, String> voWriter) {
            this(key, label, defaultValue, sort, ConfigValueTypeEnum.STRING, false, commandReader, voWriter);
        }

        Field(String key, String label, String defaultValue, int sort, ConfigValueTypeEnum valueType,
              Function<SaveAdminBrandingCommand, String> commandReader,
              BiConsumer<AdminBrandingVO, String> voWriter) {
            this(key, label, defaultValue, sort, valueType, false, commandReader, voWriter);
        }

        Field(String key, String label, String defaultValue, int sort, boolean fileField,
              Function<SaveAdminBrandingCommand, String> commandReader,
              BiConsumer<AdminBrandingVO, String> voWriter) {
            this(key, label, defaultValue, sort, ConfigValueTypeEnum.STRING, fileField, commandReader, voWriter);
        }

        String readFrom(SaveAdminBrandingCommand command) {
            return commandReader.apply(command);
        }

        void applyTo(AdminBrandingVO vo, String value) {
            voWriter.accept(vo, value);
        }
    }
}
