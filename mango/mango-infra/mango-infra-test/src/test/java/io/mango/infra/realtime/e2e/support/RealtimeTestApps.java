package io.mango.infra.realtime.e2e.support;

import io.mango.infra.realtime.e2e.apps.local.RealtimeLocalTestApplication;
import io.mango.infra.realtime.e2e.apps.remote.RealtimeRemoteTestApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.net.ServerSocket;

public final class RealtimeTestApps {
    private RealtimeTestApps() {
    }

    public static StartedApps startLocalAndRemote() {
        int localPort = findFreePort();
        int remotePort = findFreePort();
        SharedRealtimePresence.reset();

        ConfigurableApplicationContext localContext = new SpringApplicationBuilder(RealtimeLocalTestApplication.class)
                .web(WebApplicationType.SERVLET)
                .properties(
                        "spring.profiles.active=realtime-local-test",
                        "server.port=" + localPort,
                        "spring.cloud.discovery.enabled=false")
                .run();

        ConfigurableApplicationContext remoteContext = new SpringApplicationBuilder(RealtimeRemoteTestApplication.class)
                .web(WebApplicationType.SERVLET)
                .properties(
                        "spring.profiles.active=realtime-remote-test",
                        "server.port=" + remotePort,
                        "mango.infra.realtime.inbound.remote.service-name=localhost:" + remotePort,
                        "spring.cloud.discovery.client.simple.instances.mango-infra-realtime[0].uri=http://localhost:" + localPort)
                .run();

        return new StartedApps(localContext, remoteContext, localPort, remotePort);
    }

    public static StartedRealtimeNodes startTwoRealtimeNodes() {
        int nodeAPort = findFreePort();
        int nodeBPort = findFreePort();
        SharedRealtimePresence.reset();

        ConfigurableApplicationContext nodeAContext = startRealtimeNode("node-a", nodeAPort);
        ConfigurableApplicationContext nodeBContext = startRealtimeNode("node-b", nodeBPort);

        return new StartedRealtimeNodes(nodeAContext, nodeBContext, nodeAPort, nodeBPort);
    }

    private static ConfigurableApplicationContext startRealtimeNode(String nodeId, int port) {
        return new SpringApplicationBuilder(RealtimeLocalTestApplication.class)
                .web(WebApplicationType.SERVLET)
                .properties(
                        "spring.profiles.active=realtime-local-test",
                        "server.port=" + port,
                        "spring.application.name=" + nodeId,
                        "mango.infra.realtime.node.instance-id=" + nodeId,
                        "mango.infra.realtime.node.service-name=localhost:" + port,
                        "spring.cloud.discovery.enabled=false")
                .run();
    }

    private static int findFreePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to allocate free port", e);
        }
    }

    public record StartedApps(ConfigurableApplicationContext localContext,
                              ConfigurableApplicationContext remoteContext,
                              int localPort,
                              int remotePort) implements AutoCloseable {

        @Override
        public void close() {
            if (remoteContext != null) {
                remoteContext.close();
            }
            if (localContext != null) {
                localContext.close();
            }
        }
    }

    public record StartedRealtimeNodes(ConfigurableApplicationContext nodeAContext,
                                       ConfigurableApplicationContext nodeBContext,
                                       int nodeAPort,
                                       int nodeBPort) implements AutoCloseable {

        @Override
        public void close() {
            if (nodeBContext != null) {
                nodeBContext.close();
            }
            if (nodeAContext != null) {
                nodeAContext.close();
            }
        }
    }
}
