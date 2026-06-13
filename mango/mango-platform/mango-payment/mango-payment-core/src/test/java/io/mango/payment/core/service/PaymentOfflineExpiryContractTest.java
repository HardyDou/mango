package io.mango.payment.core.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentOfflineExpiryContractTest {

    @Test
    @DisplayName("expired order close scan should exclude offline collection orders")
    void expiredOrderCloseScan_shouldExcludeOfflineCollectionOrders() throws IOException {
        String statement = statement("PaymentOrderMapper", "selectExpiredOpenPaymentOrders");

        assertThat(statement)
                .contains("po.channel_code != 'OFFLINE_COLLECTION'");
    }

    private String statement(String mapperName, String statementId) throws IOException {
        String xml = resource("/mapper/payment/" + mapperName + ".xml");
        String start = "id=\"" + statementId + "\"";
        int idIndex = xml.indexOf(start);
        assertThat(idIndex).as(mapperName + "." + statementId + " exists").isGreaterThanOrEqualTo(0);
        int tagStart = xml.lastIndexOf('<', idIndex);
        int tagEnd = xml.indexOf('>', idIndex);
        String tagName = xml.substring(tagStart + 1, tagEnd).split("\\s+", 2)[0];
        int statementEnd = xml.indexOf("</" + tagName + ">", tagEnd);
        assertThat(statementEnd).as(mapperName + "." + statementId + " closing tag exists").isGreaterThan(tagEnd);
        return xml.substring(tagStart, statementEnd);
    }

    private String resource(String path) throws IOException {
        try (InputStream input = Objects.requireNonNull(getClass().getResourceAsStream(path), path)) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
