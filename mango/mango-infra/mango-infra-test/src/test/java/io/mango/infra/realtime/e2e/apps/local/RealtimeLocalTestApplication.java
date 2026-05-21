package io.mango.infra.realtime.e2e.apps.local;

import io.mango.infra.kv.api.IKvStore;
import io.mango.infra.realtime.e2e.support.SharedRealtimePresence;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication(scanBasePackages = "io.mango.infra.realtime.e2e.apps.local")
public class RealtimeLocalTestApplication {

    @Bean
    IKvStore realtimeTestKvStore() {
        return SharedRealtimePresence.get();
    }
}
