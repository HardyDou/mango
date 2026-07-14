package io.mango.cms.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.cms.api.command.SaveCmsAdvertisementCommand;
import io.mango.cms.api.command.UpdateCmsStatusCommand;
import io.mango.cms.api.enums.CmsAdPositionType;
import io.mango.cms.api.enums.CmsAdvertisementType;
import io.mango.cms.api.enums.CmsCode;
import io.mango.cms.api.enums.CmsStatus;
import io.mango.cms.api.query.CmsAdvertisementPageQuery;
import io.mango.cms.api.vo.CmsAdvertisementVO;
import io.mango.cms.core.entity.CmsAdDeliveryEntity;
import io.mango.cms.core.entity.CmsAdvertisementEntity;
import io.mango.cms.core.entity.CmsSiteEntity;
import io.mango.cms.core.mapper.CmsAdDeliveryMapper;
import io.mango.cms.core.mapper.CmsAdvertisementMapper;
import io.mango.cms.core.mapper.CmsSiteMapper;
import io.mango.cms.core.service.ICmsAdvertisementService;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.infra.persistence.api.scope.DataScopeApplier;
import io.mango.infra.persistence.api.scope.DataScopeMapping;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** CMS Advertisement aggregate service. */
@SuppressWarnings("PMD.ServiceOrDaoClassShouldEndWithImplRule")
@Service
@RequiredArgsConstructor
public class CmsAdvertisementService implements ICmsAdvertisementService {

    private static final String LIST_RESOURCE_SUFFIX = ":list";
    private static final DataScopeMapping SITE_SCOPE = cmsScope("cms_site");
    private static final DataScopeMapping ADVERTISEMENT_SCOPE = cmsScope("cms_advertisement");
    private final CmsSiteMapper siteMapper;
    private final CmsAdvertisementMapper advertisementMapper;
    private final CmsAdDeliveryMapper adDeliveryMapper;
    private final ObjectProvider<DataScopeApplier> dataScopeApplierProvider;

    @Override
    public PageResult<CmsAdvertisementVO> pageAdvertisements(CmsAdvertisementPageQuery query) {
        CmsAdvertisementPageQuery resolved = query == null ? new CmsAdvertisementPageQuery() : query;
        IPage<CmsAdvertisementEntity> page = advertisementMapper.selectPage(new Page<>(resolved.getPage(), resolved.getSize()),
                advertisementWrapper(resolved));
        return PageResult.of(page.getRecords().stream().map(this::toAdvertisementVO).toList(),
                page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public CmsAdvertisementVO detailAdvertisement(Long id) {
        return toAdvertisementVO(requireAdvertisement(id));
    }

    @Override
    public Long createAdvertisement(SaveCmsAdvertisementCommand command) {
        Require.notNull(command, CmsCode.CMS_BUSINESS_ERROR, "广告保存命令不能为空");
        CmsAdvertisementEntity entity = new CmsAdvertisementEntity();
        applyAdvertisement(entity, command, false);
        entity.setTenantId(CmsSupport.currentTenantId());
        advertisementMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public Boolean updateAdvertisement(SaveCmsAdvertisementCommand command) {
        Require.notNull(command, CmsCode.CMS_BUSINESS_ERROR, "广告保存命令不能为空");
        CmsAdvertisementEntity entity = requireAdvertisement(command.getId());
        applyAdvertisement(entity, command, true);
        return advertisementMapper.updateById(entity) > 0;
    }

    @Override
    public Boolean updateAdvertisementStatus(UpdateCmsStatusCommand command) {
        Require.notNull(command, CmsCode.CMS_BUSINESS_ERROR, "广告状态更新命令不能为空");
        CmsAdvertisementEntity entity = requireAdvertisement(command.getId());
        entity.setStatus(CmsSupport.enumName(CmsStatus.class, command.getStatus(), "广告状态非法"));
        return advertisementMapper.updateById(entity) > 0;
    }

    @Override
    public Boolean deleteAdvertisement(Long id) {
        CmsAdvertisementEntity entity = requireAdvertisement(id);
        Long deliveryCount = adDeliveryMapper.selectCount(new LambdaQueryWrapper<CmsAdDeliveryEntity>()
                .eq(CmsAdDeliveryEntity::getTenantId, CmsSupport.currentTenantId())
                .eq(CmsAdDeliveryEntity::getAdId, id));
        Require.isTrue(deliveryCount == 0, CmsCode.CMS_BUSINESS_ERROR, "广告位存在投放记录，不能删除");
        return advertisementMapper.deleteById(entity.getId()) > 0;
    }

    private void applyAdvertisement(CmsAdvertisementEntity entity, SaveCmsAdvertisementCommand command, boolean update) {
        Require.notNull(command, CmsCode.CMS_BUSINESS_ERROR, "广告保存命令不能为空");
        if (update) {
            Require.notNull(command.getId(), CmsCode.CMS_BUSINESS_ERROR, "广告 ID 不能为空");
        }
        requireSite(command.getSiteId());
        String code = CmsSupport.trimRequired(command.getAdCode(), "广告位编码不能为空");
        CmsAdvertisementEntity exists = advertisementMapper.selectOne(new LambdaQueryWrapper<CmsAdvertisementEntity>()
                .eq(CmsAdvertisementEntity::getTenantId, CmsSupport.currentTenantId())
                .eq(CmsAdvertisementEntity::getSiteId, command.getSiteId())
                .eq(CmsAdvertisementEntity::getAdCode, code)
                .last("LIMIT 1"));
        Require.isTrue(exists == null || exists.getId().equals(entity.getId()), CmsCode.CMS_BUSINESS_ERROR, "广告位编码已存在");
        entity.setSiteId(command.getSiteId());
        entity.setAdCode(code);
        entity.setAdName(CmsSupport.trimRequired(command.getAdName(), "广告位名称不能为空"));
        entity.setPosition(CmsSupport.trimRequired(command.getPosition(), "广告位位置不能为空"));
        entity.setPositionType(CmsSupport.enumName(CmsAdPositionType.class, command.getPositionType(), "位置类型非法"));
        entity.setSupportedMaterialTypes(CmsSupport.trimToNull(command.getSupportedMaterialTypes()));
        entity.setWidth(command.getWidth());
        entity.setHeight(command.getHeight());
        entity.setRemark(CmsSupport.trimToNull(command.getRemark()));
        entity.setAdType(CmsAdvertisementType.SINGLE_IMAGE.name());
        entity.setMaterialFileId(null);
        entity.setJumpUrl(null);
        entity.setStartTime(null);
        entity.setEndTime(null);
        entity.setSort(CmsSupport.defaultSort(command.getSort()));
        entity.setStatus(CmsSupport.defaultStatus(command.getStatus()));
    }

    private QueryWrapper<CmsAdvertisementEntity> advertisementWrapper(CmsAdvertisementPageQuery query) {
        String keyword = CmsSupport.trimToNull(query.getKeyword());
        QueryWrapper<CmsAdvertisementEntity> wrapper = new QueryWrapper<CmsAdvertisementEntity>()
                .eq("tenant_id", CmsSupport.currentTenantId())
                .eq(query.getSiteId() != null, "site_id", query.getSiteId())
                .eq(StringUtils.hasText(query.getPosition()), "position", query.getPosition())
                .eq(StringUtils.hasText(query.getStatus()), "status", query.getStatus())
                .and(StringUtils.hasText(keyword), w -> w.like("ad_code", keyword).or().like("ad_name", keyword))
                .orderByAsc("sort");
        return applyDataScope(wrapper, "cms:advertisement:list", ADVERTISEMENT_SCOPE);
    }

    private CmsAdvertisementEntity requireAdvertisement(Long id) {
        Require.notNull(id, CmsCode.CMS_BUSINESS_ERROR, "广告 ID 不能为空");
        CmsAdvertisementEntity entity = advertisementMapper.selectOne(scopedById(id, "cms:advertisement:list", ADVERTISEMENT_SCOPE));
        Require.notNull(entity, CmsCode.CMS_BUSINESS_ERROR, "广告不存在");
        return entity;
    }

    private CmsAdvertisementVO toAdvertisementVO(CmsAdvertisementEntity e) {
        CmsAdvertisementVO vo = new CmsAdvertisementVO();
        vo.setId(e.getId());
        vo.setSiteId(e.getSiteId());
        vo.setAdCode(e.getAdCode());
        vo.setAdName(e.getAdName());
        vo.setPosition(e.getPosition());
        vo.setPositionType(e.getPositionType());
        vo.setSupportedMaterialTypes(e.getSupportedMaterialTypes());
        vo.setWidth(e.getWidth());
        vo.setHeight(e.getHeight());
        vo.setRemark(e.getRemark());
        vo.setSort(e.getSort());
        vo.setStatus(e.getStatus());
        vo.setCreatedAt(e.getCreatedAt());
        vo.setUpdatedAt(e.getUpdatedAt());
        return vo;
    }

    private CmsSiteEntity requireSite(Long id) {
        Require.notNull(id, CmsCode.CMS_BUSINESS_ERROR, "站点 ID 不能为空");
        CmsSiteEntity entity = siteMapper.selectOne(scopedById(id, "cms:site:list", SITE_SCOPE));
        Require.notNull(entity, CmsCode.CMS_BUSINESS_ERROR, "站点不存在");
        return entity;
    }

    private <T> QueryWrapper<T> scopedById(Long id, String resourceCode, DataScopeMapping mapping) {
        QueryWrapper<T> wrapper = new QueryWrapper<T>()
                .eq("tenant_id", CmsSupport.currentTenantId())
                .eq("id", id)
                .last("LIMIT 1");
        return applyDataScope(wrapper, resourceCode, mapping);
    }

    private <T> QueryWrapper<T> applyDataScope(QueryWrapper<T> wrapper, String resourceCode, DataScopeMapping mapping) {
        DataScopeApplier dataScopeApplier = dataScopeApplierProvider.getIfAvailable();
        if (dataScopeApplier != null) {
            dataScopeApplier.apply(wrapper, resourceCode, mapping);
        }
        return wrapper;
    }

    private static DataScopeMapping cmsScope(String tableName) {
        return DataScopeMapping.builder()
                .tableName(tableName)
                .selfField("created_by")
                .orgField("org_id")
                .tenantField("tenant_id")
                .build();
    }
}
