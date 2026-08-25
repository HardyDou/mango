package io.mango.resource.core.sync;

import io.mango.resource.api.enums.ResourceApplyMode;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Objects;

/** Persists the last successfully installed complete state of each Resource module. */
public final class ResourceModuleReceiptRepository {

    private final JdbcTemplate jdbcTemplate;

    public ResourceModuleReceiptRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
    }

    public boolean isSatisfied(String environmentKey, String appCode, String serviceCode,
                               String moduleCode, String moduleHash, ResourceApplyMode applyMode) {
        String requiredState = applyMode == ResourceApplyMode.FINALIZE ? "FINALIZED" : "EXPANDED";
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM resource_module_receipt
                 WHERE environment_key = ? AND app_code = ? AND service_code = ? AND module_code = ?
                   AND module_hash = ?
                   AND (state = 'FINALIZED' OR state = ?)
                """, Integer.class, environmentKey, appCode, serviceCode, moduleCode, moduleHash, requiredState);
        return count != null && count == 1;
    }

    public void recordSuccess(String environmentKey, String appCode, String serviceCode,
                              String moduleCode, String moduleHash, long generation,
                              String manifestFingerprint, ResourceApplyMode applyMode, int declarationCount) {
        String state = applyMode == ResourceApplyMode.FINALIZE ? "FINALIZED" : "EXPANDED";
        int updated = jdbcTemplate.update("""
                UPDATE resource_module_receipt
                   SET module_hash = ?, generation = ?, manifest_fingerprint = ?, state = ?,
                       declaration_count = ?, updated_at = CURRENT_TIMESTAMP
                 WHERE environment_key = ? AND app_code = ? AND service_code = ? AND module_code = ?
                """, moduleHash, generation, manifestFingerprint, state, declarationCount,
                environmentKey, appCode, serviceCode, moduleCode);
        if (updated == 0) {
            jdbcTemplate.update("""
                    INSERT INTO resource_module_receipt
                    (environment_key, app_code, service_code, module_code, module_hash, generation,
                     manifest_fingerprint, state, declaration_count)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, environmentKey, appCode, serviceCode, moduleCode, moduleHash, generation,
                    manifestFingerprint, state, declarationCount);
        }
    }
}
