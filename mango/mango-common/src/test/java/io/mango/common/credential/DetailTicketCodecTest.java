package io.mango.common.credential;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DetailTicketCodecTest {

    private static final byte[] SECRET = "mango-detail-ticket-secret".getBytes(StandardCharsets.UTF_8);
    private static final String SUBJECT = "tenant-1|1001|scope-abc";

    @Test
    void encodeAndDecodeRoundTrip() {
        DetailTicket ticket = new DetailTicket("3", 1_771_000_000L,
                List.of("G:/cms/open/contents/detail"), "103");

        String token = DetailTicketCodec.encode(ticket, SUBJECT, SECRET);
        DetailTicket decoded = DetailTicketCodec.decode(token, SUBJECT, kid -> SECRET);

        assertThat(token).startsWith("mdt1.3.1771000000.");
        assertThat(decoded.endpoints()).containsExactly("G:/cms/open/contents/detail");
        assertThat(decoded.dataId()).isEqualTo("103");
        assertThat(decoded.issuedAt()).isEqualTo(1_771_000_000L);
    }

    @Test
    void tamperedDataIdIsRejected() {
        String token = DetailTicketCodec.encode(new DetailTicket("3", 1_771_000_000L,
                List.of("G:/cms/open/contents/detail"), "103"), SUBJECT, SECRET);
        String tampered = token.substring(0, token.length() - 3) + "104";

        assertThatThrownBy(() -> DetailTicketCodec.decode(tampered, SUBJECT, kid -> SECRET))
                .isInstanceOf(DetailTicketException.class)
                .extracting(exception -> ((DetailTicketException) exception).reason())
                .isEqualTo(DetailTicketException.Reason.INVALID);
    }

    @Test
    void tamperedEndpointSetIsRejected() {
        String token = DetailTicketCodec.encode(new DetailTicket("3", 1_771_000_000L,
                List.of("G:/cms/open/contents/detail"), "103"), SUBJECT, SECRET);
        String tampered = token.replace("G:/cms/open/contents/detail", "G:/cms/contents/detail");

        assertThatThrownBy(() -> DetailTicketCodec.decode(tampered, SUBJECT, kid -> SECRET))
                .isInstanceOf(DetailTicketException.class);
    }

    @Test
    void otherSubjectCannotReplayTicket() {
        String token = DetailTicketCodec.encode(new DetailTicket("3", 1_771_000_000L,
                List.of("G:/cms/open/contents/detail"), "103"), SUBJECT, SECRET);

        assertThatThrownBy(() -> DetailTicketCodec.decode(token, "tenant-2|2002|scope-abc", kid -> SECRET))
                .isInstanceOf(DetailTicketException.class)
                .extracting(exception -> ((DetailTicketException) exception).reason())
                .isEqualTo(DetailTicketException.Reason.INVALID);
    }

    @Test
    void unknownKeyVersionIsRejected() {
        String token = DetailTicketCodec.encode(new DetailTicket("3", 1_771_000_000L,
                List.of("G:/cms/open/contents/detail"), "103"), SUBJECT, SECRET);

        assertThatThrownBy(() -> DetailTicketCodec.decode(token, SUBJECT, kid -> null))
                .isInstanceOf(DetailTicketException.class);
    }

    @Test
    void missingTokenIsReportedAsMissing() {
        assertThatThrownBy(() -> DetailTicketCodec.decode(" ", SUBJECT, kid -> SECRET))
                .isInstanceOf(DetailTicketException.class)
                .extracting(exception -> ((DetailTicketException) exception).reason())
                .isEqualTo(DetailTicketException.Reason.MISSING);
    }

    @Test
    void endpointKeyNormalizesMethodAndPath() {
        assertThat(DetailTicketEndpoints.fromDeclaration("GET /cms/open/contents/detail"))
                .isEqualTo("G:/cms/open/contents/detail");
        assertThat(DetailTicketEndpoints.key("delete", "/cms/contents")).isEqualTo("D:/cms/contents");
    }

    @Test
    void endpointDeclarationRejectsAmbiguousCharacters() {
        assertThatThrownBy(() -> DetailTicketEndpoints.fromDeclaration("GET /cms/v1.0/detail"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DetailTicketEndpoints.fromDeclaration("GET /cms/a~b"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ticketAllowsOnlyDeclaredEndpoints() {
        DetailTicket ticket = new DetailTicket("3", 1_771_000_000L,
                List.of("G:/cms/open/contents/detail", "G:/cms/contents/detail"), "103");

        assertThat(ticket.allows("G:/cms/contents/detail")).isTrue();
        assertThat(ticket.allows("G:/cms/contents")).isFalse();
    }
}
