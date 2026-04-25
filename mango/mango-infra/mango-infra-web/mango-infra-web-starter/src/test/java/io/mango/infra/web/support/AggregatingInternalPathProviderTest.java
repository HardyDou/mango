package io.mango.infra.web.support;

import io.mango.infra.web.api.IInternalPathProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.support.GenericApplicationContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AggregatingInternalPathProviderTest {

    @Test
    void getInternalPathsShouldMergeProvidersAndIgnoreSelf() {
        GenericApplicationContext context = new GenericApplicationContext(new DefaultListableBeanFactory());
        context.registerBean("first", IInternalPathProvider.class, () -> () -> List.of("/a/**", "/b"));
        context.registerBean("second", IInternalPathProvider.class, () -> () -> List.of("/b", "/c"));
        context.refresh();

        AggregatingInternalPathProvider provider = new AggregatingInternalPathProvider(
                context.getBeanProvider(IInternalPathProvider.class));

        assertEquals(List.of("/a/**", "/b", "/c"), provider.getInternalPaths());
        context.close();
    }
}
