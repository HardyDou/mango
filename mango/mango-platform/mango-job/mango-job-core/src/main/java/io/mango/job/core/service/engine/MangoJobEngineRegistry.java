package io.mango.job.core.service.engine;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 默认 Mango Job 引擎注册表。
 */
@Service
public class MangoJobEngineRegistry implements IMangoJobEngineRegistry {

    private final Map<String, IMangoJobEngine> engines;

    public MangoJobEngineRegistry(ObjectProvider<IMangoJobEngine> provider) {
        this.engines = provider.stream()
                .collect(Collectors.toUnmodifiableMap(
                        engine -> normalize(engine.engineType()),
                        Function.identity(),
                        (left, right) -> left));
    }

    @Override
    public Optional<IMangoJobEngine> findEngine(String engineType) {
        return Optional.ofNullable(engines.get(normalize(engineType)));
    }

    private static String normalize(String engineType) {
        return engineType == null ? "" : engineType.trim().toUpperCase(Locale.ROOT);
    }
}
