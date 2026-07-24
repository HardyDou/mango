package io.mango.notice.core.service;

import io.mango.notice.support.channel.NoticeChannelSecretResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DefaultNoticeChannelSecretResolver implements NoticeChannelSecretResolver {

    private final Environment environment;

    @Override
    public boolean supports(String reference) {
        return reference != null && (reference.startsWith("env:") || reference.startsWith("property:"));
    }

    @Override
    public String resolve(String reference) {
        if (reference.startsWith("env:")) {
            return System.getenv(reference.substring("env:".length()));
        }
        return environment.getProperty(reference.substring("property:".length()));
    }
}
