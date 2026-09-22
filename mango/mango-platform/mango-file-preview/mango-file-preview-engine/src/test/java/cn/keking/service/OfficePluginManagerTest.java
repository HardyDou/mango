package cn.keking.service;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OfficePluginManagerTest {

    @Test
    void workerCountGeneratesTheSameNumberOfOfficePorts() throws Exception {
        OfficePluginManager manager = new OfficePluginManager();
        set(manager, "serverPorts", "");
        set(manager, "conversionWorkerCount", 10);
        set(manager, "officeBasePort", 2001);

        int[] ports = resolvePorts(manager);

        assertThat(ports).hasSize(10);
        assertThat(ports[0]).isEqualTo(2001);
        assertThat(ports[9]).isEqualTo(2010);
    }

    @Test
    void explicitOfficePortCountMustMatchWorkerCount() throws Exception {
        OfficePluginManager manager = new OfficePluginManager();
        set(manager, "serverPorts", "2001,2002");
        set(manager, "conversionWorkerCount", 10);
        set(manager, "officeBasePort", 2001);

        assertThatThrownBy(() -> resolvePorts(manager))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("数量必须与转换 worker 数量一致");
    }

    private int[] resolvePorts(OfficePluginManager manager) throws Exception {
        Method method = OfficePluginManager.class.getDeclaredMethod("resolvePorts");
        method.setAccessible(true);
        try {
            return (int[]) method.invoke(manager);
        } catch (InvocationTargetException ex) {
            if (ex.getCause() instanceof Exception cause) {
                throw cause;
            }
            throw ex;
        }
    }

    private void set(OfficePluginManager manager, String fieldName, Object value) throws Exception {
        var field = OfficePluginManager.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(manager, value);
    }
}
