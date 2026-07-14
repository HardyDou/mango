package io.mango.payment.starter.endpoint;

import io.mango.payment.core.service.IPaymentChannelCallbackHandlerService;
import io.mango.payment.core.service.PaymentChannelCallbackHandleResult;
import io.mango.payment.core.service.PaymentChannelRawCallback;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.web.servlet.function.RequestPredicates.GET;
import static org.springframework.web.servlet.function.RequestPredicates.POST;
import static org.springframework.web.servlet.function.RouterFunctions.route;

class PaymentChannelPublicCallbackEndpointTest {

    @Test
    void publicRoutePreservesRawBodyMetadataAndPlainTextGatewayAck() throws Exception {
        IPaymentChannelCallbackHandlerService handlerService = mock(IPaymentChannelCallbackHandlerService.class);
        when(handlerService.handle(org.mockito.ArgumentMatchers.any()))
                .thenReturn(PaymentChannelCallbackHandleResult.text("success"));
        PaymentChannelPublicCallbackEndpoint endpoint = new PaymentChannelPublicCallbackEndpoint(handlerService);
        MockMvc mvc = MockMvcBuilders.routerFunctions(route(
                GET("/payment/channel-callbacks/public")
                        .or(POST("/payment/channel-callbacks/public")), endpoint::handle)).build();

        mvc.perform(post("/payment/channel-callbacks/public")
                        .queryParam("channelCode", "FUIOU_PAY")
                        .queryParam("sign", "signed-value")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content("{\"orderId\":\"P-1\"}")
                        .with(request -> {
                            request.setRemoteAddr("203.0.113.10");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/plain;charset=UTF-8"))
                .andExpect(content().string("success"));

        ArgumentCaptor<PaymentChannelRawCallback> callbackCaptor =
                ArgumentCaptor.forClass(PaymentChannelRawCallback.class);
        verify(handlerService).handle(callbackCaptor.capture());
        PaymentChannelRawCallback callback = callbackCaptor.getValue();
        assertThat(callback.channelCode()).isEqualTo("FUIOU_PAY");
        assertThat(callback.method()).isEqualTo("POST");
        assertThat(callback.uri()).isEqualTo("/payment/channel-callbacks/public");
        assertThat(callback.remoteAddr()).isEqualTo("203.0.113.10");
        assertThat(callback.params()).containsEntry("sign", "signed-value");
        assertThat(callback.rawBody()).isEqualTo("{\"orderId\":\"P-1\"}");
        assertThat(callback.receivedAt()).isNotNull();
    }
}
