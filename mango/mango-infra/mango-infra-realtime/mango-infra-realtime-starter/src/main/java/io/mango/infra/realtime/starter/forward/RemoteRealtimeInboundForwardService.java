package io.mango.infra.realtime.starter.forward;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.mango.infra.realtime.api.dto.RealtimeInboundMessage;
import io.mango.infra.realtime.api.dto.RealtimeInboundReceiverRegistration;
import io.mango.infra.realtime.core.inbound.receiver.IRealtimeInboundReceiverService;
import io.mango.infra.realtime.core.inbound.forward.IRealtimeInboundForwardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "Inbound registry and RestOperations are injected Spring singleton collaborators"))
public class RemoteRealtimeInboundForwardService implements IRealtimeInboundForwardService {

    private final IRealtimeInboundReceiverService realtimeInboundReceiverService;
    private final RestOperations restOperations;

    @Override
    public void forward(RealtimeInboundMessage message) {
        for (RealtimeInboundReceiverRegistration registration : realtimeInboundReceiverService.findAll()) {
            try {
                restOperations.postForEntity(receiverUrl(registration), message, Void.class);
            } catch (RestClientException e) {
                log.warn("Failed to forward realtime inbound message {} to service {}",
                        message.id(), registration.serviceName(), e);
            }
        }
    }

    private String receiverUrl(RealtimeInboundReceiverRegistration registration) {
        return "http://" + registration.serviceName()
                + normalizeContextPath(registration.contextPath())
                + registration.endpoint();
    }

    private String normalizeContextPath(String contextPath) {
        if (contextPath == null || contextPath.isBlank() || "/".equals(contextPath)) {
            return "";
        }
        if (contextPath.startsWith("/")) {
            return contextPath;
        }
        return "/" + contextPath;
    }
}
