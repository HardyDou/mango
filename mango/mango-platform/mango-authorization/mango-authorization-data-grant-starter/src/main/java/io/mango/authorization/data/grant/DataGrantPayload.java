package io.mango.authorization.data.grant;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * Grant 载荷：一次列表查询返回的记录 ID 集合及其归属上下文。
 * <p>
 * 载荷只承载「谁、在哪个租户、哪个数据域、能看到哪些 ID、什么时候过期」，
 * 不承载角色、菜单或数据范围规则本身；数据范围变化通过 {@code permissionVersion} 指纹感知。
 *
 * @param tenantId          租户标识。
 * @param memberId          成员标识（账号维度）。
 * @param domain            数据权限域标识。
 * @param ids               可见记录 ID 集合，升序、去重、不可变。
 * @param issuedAt          签发时间（epoch 秒）。
 * @param expiresAt         过期时间（epoch 秒）。
 * @param permissionVersion 数据范围指纹，可为 null（表示签发时未解析到数据范围规则）。
 */
public record DataGrantPayload(
        String tenantId,
        Long memberId,
        String domain,
        Set<Long> ids,
        long issuedAt,
        long expiresAt,
        String permissionVersion) {

    /**
     * 归一化不可变状态：ID 集合复制为升序不可变集合，空白指纹归一为 null。
     */
    public DataGrantPayload {
        ids = ids == null ? Set.of() : Collections.unmodifiableSet(new TreeSet<>(ids));
        permissionVersion = permissionVersion == null || permissionVersion.isBlank() ? null : permissionVersion;
    }

    /**
     * 判断记录 ID 是否在可见集合内。
     *
     * @param recordId 记录 ID，可为 null。
     * @return 命中集合时返回 true。
     */
    public boolean containsId(Long recordId) {
        return recordId != null && ids.contains(recordId);
    }
}
