package io.mango.infra.crypto;

import io.mango.infra.crypto.impl.IAsymmetricCryptoService;
import io.mango.infra.crypto.impl.ICryptoService;
import io.mango.infra.crypto.impl.IDigester;
import io.mango.infra.crypto.impl.IKeyedDigester;
import io.mango.infra.crypto.impl.ISignService;
import io.mango.infra.crypto.impl.sm.Sm2SignService;
import io.mango.infra.crypto.impl.sm.Sm4CryptoService;
import io.mango.infra.crypto.starter.CryptoProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CryptoPublicContractCompatibilityTest {

    @Test
    void publicInterfaces_preserveMethodSignatures() {
        assertEquals(Set.of(
                        "encrypt(String):String",
                        "encrypt(String,String):String",
                        "decrypt(String):String",
                        "decrypt(String,String):String"),
                signatures(ICryptoService.class));
        assertEquals(Set.of("encrypt(String):String"), signatures(IAsymmetricCryptoService.class));
        assertEquals(Set.of("digest(String):String", "digest(byte[]):byte[]"),
                signatures(IDigester.class));
        assertEquals(Set.of("digest(String,byte[]):String", "digest(byte[],byte[]):byte[]"),
                signatures(IKeyedDigester.class));
        assertEquals(Set.of("sign(String):String", "verify(String,String):boolean"),
                signatures(ISignService.class));
    }

    @Test
    void publicConstructionAndConfigurationPrefix_remainCompatible() {
        assertDoesNotThrow(() -> Sm2SignService.class.getConstructor(CryptoProperties.class));
        assertDoesNotThrow(() -> Sm4CryptoService.class.getConstructor(CryptoProperties.class));
        ConfigurationProperties annotation =
                CryptoProperties.class.getAnnotation(ConfigurationProperties.class);
        assertEquals("mango.crypto", annotation.prefix());
    }

    private Set<String> signatures(Class<?> contract) {
        return Arrays.stream(contract.getDeclaredMethods())
                .map(this::signature)
                .collect(Collectors.toSet());
    }

    private String signature(Method method) {
        String parameters = Arrays.stream(method.getParameterTypes())
                .map(Class::getSimpleName)
                .collect(Collectors.joining(","));
        return method.getName() + "(" + parameters + "):" + method.getReturnType().getSimpleName();
    }
}
