package io.mango.file.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.common.result.R;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.file.api.FileCode;
import io.mango.file.api.command.SaveFileStorageConfigCommand;
import io.mango.file.api.command.TestFileStorageConfigCommand;
import io.mango.file.api.enums.FileStorageType;
import io.mango.file.api.query.FileStorageConfigPageQuery;
import io.mango.file.api.vo.FileStorageConfigTestVO;
import io.mango.file.api.vo.FileStorageConfigVO;
import io.mango.file.core.config.FileProperties;
import io.mango.file.core.entity.FileStorageConfig;
import io.mango.file.core.mapper.FileStorageConfigMapper;
import io.mango.file.core.service.IFileStorageConfigService;
import io.mango.file.core.storage.FileStorageRouter;
import io.mango.infra.context.core.MangoContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * 文件存储配置服务实现。
 */
@Service
@RequiredArgsConstructor
public class FileStorageConfigServiceImpl implements IFileStorageConfigService {

    private final FileStorageConfigMapper mapper;
    private final FileProperties properties;
    private final FileStorageRouter storageRouter;

    @Override
    public R<PageResult<FileStorageConfigVO>> page(FileStorageConfigPageQuery query) {
        FileStorageConfigPageQuery resolved = query == null ? new FileStorageConfigPageQuery() : query;
        IPage<FileStorageConfig> page = mapper.selectPage(
                new Page<>(resolved.getPage(), resolved.getSize()),
                wrapper(resolved));
        List<FileStorageConfigVO> records = page.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return R.ok(PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize()));
    }

    @Override
    public R<FileStorageConfigVO> get(Long id) {
        return R.ok(toVO(selectRequired(id)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Long> create(SaveFileStorageConfigCommand command) {
        Require.notNull(command, FileCode.STORAGE_CONFIG_INVALID);
        validate(command, false);
        FileStorageConfig entity = new FileStorageConfig();
        copy(command, entity, false);
        LocalDateTime now = LocalDateTime.now();
        entity.setCreatedTime(now);
        entity.setUpdatedTime(now);
        entity.setCreatedBy(MangoContextHolder.userId());
        entity.setUpdatedBy(MangoContextHolder.userId());
        if (Integer.valueOf(1).equals(entity.getActive())) {
            clearActive(null);
        }
        mapper.insert(entity);
        return R.ok(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> update(SaveFileStorageConfigCommand command) {
        Require.notNull(command, FileCode.STORAGE_CONFIG_INVALID);
        Require.notNull(command.getId(), FileCode.STORAGE_CONFIG_INVALID.getCode(), "配置ID不能为空");
        validate(command, true);
        FileStorageConfig entity = selectRequired(command.getId());
        copy(command, entity, true);
        entity.setUpdatedBy(MangoContextHolder.userId());
        entity.setUpdatedTime(LocalDateTime.now());
        if (Integer.valueOf(1).equals(entity.getActive())) {
            clearActive(entity.getId());
        }
        return R.ok(mapper.updateById(entity) > 0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> delete(Long id) {
        FileStorageConfig entity = selectRequired(id);
        Require.isFalse(Integer.valueOf(1).equals(entity.getActive()), FileCode.STORAGE_CONFIG_INVALID.getCode(), "默认启用配置不能删除");
        return R.ok(mapper.deleteById(id) > 0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> activate(Long id) {
        FileStorageConfig entity = selectRequired(id);
        Require.isTrue(Integer.valueOf(1).equals(entity.getStatus()), FileCode.STORAGE_CONFIG_DISABLED);
        clearActive(id);
        entity.setActive(1);
        entity.setUpdatedBy(MangoContextHolder.userId());
        entity.setUpdatedTime(LocalDateTime.now());
        return R.ok(mapper.updateById(entity) > 0);
    }

    @Override
    public R<FileStorageConfigTestVO> test(TestFileStorageConfigCommand command) {
        Require.notNull(command, FileCode.STORAGE_CONFIG_INVALID);
        FileStorageConfig config;
        if (command.getId() != null) {
            config = selectRequired(command.getId());
        } else {
            Require.notNull(command.getConfig(), FileCode.STORAGE_CONFIG_INVALID);
            validate(command.getConfig(), false);
            config = new FileStorageConfig();
            copy(command.getConfig(), config, false);
        }
        try {
            storageRouter.test(config);
            FileStorageConfigTestVO vo = new FileStorageConfigTestVO();
            vo.setSuccess(true);
            vo.setMessage("连接测试通过");
            return R.ok(vo);
        } catch (Exception e) {
            return Require.fail(FileCode.STORAGE_CONFIG_TEST_FAILED.getCode(), "连接测试失败：" + e.getMessage());
        }
    }

    @Override
    public FileStorageConfig activeConfig() {
        FileStorageConfig entity = mapper.selectOne(new LambdaQueryWrapper<FileStorageConfig>()
                .eq(FileStorageConfig::getActive, 1)
                .eq(FileStorageConfig::getStatus, 1)
                .last("LIMIT 1"));
        if (entity != null) {
            return entity;
        }
        FileStorageConfig fallback = new FileStorageConfig();
        fallback.setId(0L);
        fallback.setConfigName("配置文件默认存储");
        fallback.setStorageType(properties.getStorageType().name());
        fallback.setBucketName(properties.getDefaultBucket());
        fallback.setActive(1);
        fallback.setStatus(1);
        fallback.setPathStyleAccess(0);
        fallback.setSslEnabled(0);
        return fallback;
    }

    @Override
    public FileStorageConfig getEnabledConfig(Long id, String storageType, String bucketName) {
        FileStorageConfig entity = null;
        if (id != null && id > 0) {
            entity = mapper.selectById(id);
        }
        if (entity == null && StringUtils.hasText(storageType) && StringUtils.hasText(bucketName)) {
            entity = mapper.selectOne(new LambdaQueryWrapper<FileStorageConfig>()
                    .eq(FileStorageConfig::getStorageType, storageType)
                    .eq(FileStorageConfig::getBucketName, bucketName)
                    .eq(FileStorageConfig::getStatus, 1)
                    .last("LIMIT 1"));
        }
        Require.notNull(entity, FileCode.STORAGE_CONFIG_NOT_FOUND);
        Require.isTrue(Integer.valueOf(1).equals(entity.getStatus()), FileCode.STORAGE_CONFIG_DISABLED);
        return entity;
    }

    private LambdaQueryWrapper<FileStorageConfig> wrapper(FileStorageConfigPageQuery query) {
        LambdaQueryWrapper<FileStorageConfig> wrapper = new LambdaQueryWrapper<>();
        String keyword = trimToNull(query.getKeyword());
        wrapper.and(StringUtils.hasText(keyword), nested -> nested
                .like(FileStorageConfig::getConfigName, keyword)
                .or()
                .like(FileStorageConfig::getBucketName, keyword)
                .or()
                .like(FileStorageConfig::getEndpoint, keyword));
        wrapper.eq(query.getStorageType() != null, FileStorageConfig::getStorageType, query.getStorageType() == null ? null : query.getStorageType().name());
        wrapper.eq(query.getActive() != null, FileStorageConfig::getActive, Boolean.TRUE.equals(query.getActive()) ? 1 : 0);
        wrapper.eq(query.getStatus() != null, FileStorageConfig::getStatus, query.getStatus());
        wrapper.orderByDesc(FileStorageConfig::getActive).orderByDesc(FileStorageConfig::getUpdatedTime);
        return wrapper;
    }

    private FileStorageConfig selectRequired(Long id) {
        Require.notNull(id, FileCode.STORAGE_CONFIG_INVALID.getCode(), "配置ID不能为空");
        FileStorageConfig entity = mapper.selectById(id);
        Require.notNull(entity, FileCode.STORAGE_CONFIG_NOT_FOUND);
        return entity;
    }

    private void validate(SaveFileStorageConfigCommand command, boolean update) {
        Require.notBlank(command.getConfigName(), FileCode.STORAGE_CONFIG_INVALID.getCode(), "配置名称不能为空");
        Require.notNull(command.getStorageType(), FileCode.STORAGE_CONFIG_INVALID.getCode(), "存储类型不能为空");
        Require.notBlank(command.getBucketName(), FileCode.STORAGE_CONFIG_INVALID.getCode(), "存储桶不能为空");
        if (command.getStorageType() != FileStorageType.LOCAL) {
            Require.notBlank(command.getAccessKey(), FileCode.STORAGE_CONFIG_INVALID.getCode(), "AccessKey不能为空");
            Require.isTrue(update || StringUtils.hasText(command.getSecretKey()),
                    FileCode.STORAGE_CONFIG_INVALID.getCode(), "SecretKey不能为空");
        }
        if (command.getStorageType() == FileStorageType.ALIYUN_OSS || command.getStorageType() == FileStorageType.MINIO
                || command.getStorageType() == FileStorageType.S3) {
            Require.notBlank(command.getEndpoint(), FileCode.STORAGE_CONFIG_INVALID.getCode(), "接入地址不能为空");
        }
        if (command.getStorageType() == FileStorageType.TENCENT_COS) {
            Require.notBlank(command.getRegion(), FileCode.STORAGE_CONFIG_INVALID.getCode(), "腾讯云 COS 区域不能为空");
        }
    }

    private void copy(SaveFileStorageConfigCommand command, FileStorageConfig entity, boolean keepSecretWhenBlank) {
        entity.setConfigName(command.getConfigName().trim());
        entity.setStorageType(command.getStorageType().name());
        entity.setEndpoint(trimToNull(command.getEndpoint()));
        entity.setPublicEndpoint(trimToNull(command.getPublicEndpoint()));
        entity.setRegion(trimToNull(command.getRegion()));
        entity.setBucketName(command.getBucketName().trim());
        entity.setAccessKey(trimToNull(command.getAccessKey()));
        if (!keepSecretWhenBlank || StringUtils.hasText(command.getSecretKey())) {
            entity.setSecretKey(trimToNull(command.getSecretKey()));
        }
        entity.setPathStyleAccess(Boolean.TRUE.equals(command.getPathStyleAccess()) ? 1 : 0);
        entity.setSslEnabled(Boolean.TRUE.equals(command.getSslEnabled()) ? 1 : 0);
        entity.setActive(Boolean.TRUE.equals(command.getActive()) ? 1 : 0);
        entity.setStatus(command.getStatus() == null ? 1 : command.getStatus());
        entity.setRemark(trimToNull(command.getRemark()));
    }

    private void clearActive(Long excludeId) {
        List<FileStorageConfig> activeList = mapper.selectList(new LambdaQueryWrapper<FileStorageConfig>()
                .eq(FileStorageConfig::getActive, 1));
        for (FileStorageConfig item : activeList) {
            if (excludeId != null && excludeId.equals(item.getId())) {
                continue;
            }
            item.setActive(0);
            item.setUpdatedTime(LocalDateTime.now());
            mapper.updateById(item);
        }
    }

    private FileStorageConfigVO toVO(FileStorageConfig entity) {
        FileStorageConfigVO vo = new FileStorageConfigVO();
        vo.setId(entity.getId());
        vo.setConfigName(entity.getConfigName());
        vo.setStorageType(FileStorageType.valueOf(entity.getStorageType().toUpperCase(Locale.ROOT)));
        vo.setEndpoint(entity.getEndpoint());
        vo.setPublicEndpoint(entity.getPublicEndpoint());
        vo.setRegion(entity.getRegion());
        vo.setBucketName(entity.getBucketName());
        vo.setAccessKey(entity.getAccessKey());
        vo.setSecretConfigured(StringUtils.hasText(entity.getSecretKey()));
        vo.setPathStyleAccess(Integer.valueOf(1).equals(entity.getPathStyleAccess()));
        vo.setSslEnabled(Integer.valueOf(1).equals(entity.getSslEnabled()));
        vo.setActive(Integer.valueOf(1).equals(entity.getActive()));
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreatedTime(entity.getCreatedTime());
        vo.setUpdatedTime(entity.getUpdatedTime());
        return vo;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
