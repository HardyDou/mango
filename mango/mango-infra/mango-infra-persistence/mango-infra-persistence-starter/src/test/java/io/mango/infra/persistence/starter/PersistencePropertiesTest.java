package io.mango.infra.persistence.starter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("PersistenceProperties Tests")
class PersistencePropertiesTest {

    @Test
    @DisplayName("default tenant exclusions should include authorization menu package metadata")
    void defaultTenantExcludedTables_authorizationMenuPackageMetadata_included() {
        PersistenceProperties properties = new PersistenceProperties();

        assertTrue(properties.getMybatisPlus().getTenant().getExcludedTables()
                .contains("authorization_menu_package"));
        assertTrue(properties.getMybatisPlus().getTenant().getExcludedTables()
                .contains("authorization_menu_package_item"));
    }
}
