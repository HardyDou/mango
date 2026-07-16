package io.mango.resource.starter.remote;

import io.mango.infra.web.util.InternalCallSignature;
import io.mango.resource.api.command.ExecuteResourceTargetCommand;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequest;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ResourceTargetHttpClientTest {

    private static final String SECRET = "resource-target-test-secret";
    private static final long TIMESTAMP = 1_750_000_000_000L;
    private static final String NONCE = "resource-target-nonce";

    @Test
    void upsertBatch_serviceNameUri_resolvesInstancePreservesContextPathAndSignsRequest() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RecordingLoadBalancerClient loadBalancer = new RecordingLoadBalancerClient(new DefaultServiceInstance(
                "authorization-1", "authorization-service", "127.0.0.1", 18611, false));
        ResourceTargetHttpClient client = client(builder, loadBalancer);
        String path = "/admin/resource/targets/upsert-batch";
        String signature = InternalCallSignature.sign(
                Long.toString(TIMESTAMP), NONCE, "POST", path, "", SECRET);

        server.expect(once(), request -> {
                    assertThat(request.getURI()).isEqualTo(URI.create("http://127.0.0.1:18611" + path));
                    assertThat(request.getMethod()).isEqualTo(HttpMethod.POST);
                    assertThat(request.getHeaders().getFirst("X-Internal-Call")).isEqualTo("true");
                    assertThat(request.getHeaders().getFirst("X-Internal-Timestamp"))
                            .isEqualTo(Long.toString(TIMESTAMP));
                    assertThat(request.getHeaders().getFirst("X-Internal-Nonce")).isEqualTo(NONCE);
                    assertThat(request.getHeaders().getFirst("X-Internal-Secret-Version")).isEqualTo("7");
                    assertThat(request.getHeaders().getFirst("X-Internal-Signature")).isEqualTo(signature);
                })
                .andRespond(withSuccess("{\"code\":200,\"data\":{\"entries\":[]}}", MediaType.APPLICATION_JSON));

        var response = client.upsertBatch(
                URI.create("http://authorization-service/admin"), new ExecuteResourceTargetCommand());

        assertThat(response).isNotNull();
        server.verify();
        assertThat(loadBalancer.chooseCount).isEqualTo(1);
        assertThat(loadBalancer.lastServiceId).isEqualTo("authorization-service");
    }

    @Test
    void delete_explicitInstanceUri_skipsLoadBalancer() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RecordingLoadBalancerClient loadBalancer = new RecordingLoadBalancerClient(null);
        ResourceTargetHttpClient client = client(builder, loadBalancer);
        String path = "/platform/resource/targets/delete";

        server.expect(once(), request -> {
                    assertThat(request.getURI()).isEqualTo(URI.create("http://127.0.0.1:18612" + path));
                    assertThat(request.getMethod()).isEqualTo(HttpMethod.POST);
                })
                .andRespond(withSuccess("{\"code\":200,\"data\":{}}", MediaType.APPLICATION_JSON));

        var response = client.delete(
                URI.create("http://127.0.0.1:18612/platform"), new ExecuteResourceTargetCommand());

        assertThat(response).isNotNull();
        server.verify();
        assertThat(loadBalancer.chooseCount).isZero();
    }

    private ResourceTargetHttpClient client(RestClient.Builder builder, LoadBalancerClient loadBalancer) {
        return new ResourceTargetHttpClient(
                builder.build(), loadBalancer, SECRET, 7, () -> TIMESTAMP, () -> NONCE);
    }

    private static class RecordingLoadBalancerClient implements LoadBalancerClient {

        private final ServiceInstance instance;
        private int chooseCount;
        private String lastServiceId;

        private RecordingLoadBalancerClient(ServiceInstance instance) {
            this.instance = instance;
        }

        @Override
        public ServiceInstance choose(String serviceId) {
            chooseCount++;
            lastServiceId = serviceId;
            return instance;
        }

        @Override
        public <T> ServiceInstance choose(String serviceId, Request<T> request) {
            return choose(serviceId);
        }

        @Override
        public <T> T execute(String serviceId, LoadBalancerRequest<T> request) throws IOException {
            return execute(serviceId, instance, request);
        }

        @Override
        public <T> T execute(String serviceId, ServiceInstance serviceInstance,
                             LoadBalancerRequest<T> request) throws IOException {
            try {
                return request.apply(serviceInstance);
            } catch (Exception exception) {
                throw new IOException(exception);
            }
        }

        @Override
        public URI reconstructURI(ServiceInstance serviceInstance, URI original) {
            return serviceInstance.getUri();
        }
    }
}
