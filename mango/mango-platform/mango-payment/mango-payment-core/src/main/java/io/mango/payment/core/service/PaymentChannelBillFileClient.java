package io.mango.payment.core.service;

import io.mango.common.result.Require;
import io.mango.payment.api.PaymentCode;
import io.mango.payment.core.entity.PaymentChannelBillSourceEntity;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Component
public class PaymentChannelBillFileClient {

    private static final Logger log = LoggerFactory.getLogger(PaymentChannelBillFileClient.class);
    private static final int DEFAULT_FTP_PORT = 21;
    private static final int DEFAULT_FTPS_PORT = 21;
    private static final int CONNECT_TIMEOUT_MILLIS = (int) Duration.ofSeconds(15).toMillis();
    private static final int DATA_TIMEOUT_MILLIS = (int) Duration.ofSeconds(30).toMillis();

    private final List<PaymentChannelBillCredentialProvider> credentialProviders;

    public PaymentChannelBillFileClient(List<PaymentChannelBillCredentialProvider> credentialProviders) {
        this.credentialProviders = credentialProviders == null ? List.of() : List.copyOf(credentialProviders);
    }

    public RemoteBillFile fetch(PaymentChannelBillSourceEntity source) {
        String fetchMode = source.getFetchMode();
        Require.isTrue("FTP".equals(fetchMode) || "FTPS".equals(fetchMode),
                PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "账单获取方式必须是 FTP 或 FTPS");
        Endpoint endpoint = parseEndpoint(source.getEndpoint(), "FTPS".equals(fetchMode) ? DEFAULT_FTPS_PORT : DEFAULT_FTP_PORT);
        String remotePath = PaymentContextSupport.trimToNull(source.getRemotePath());
        Require.notBlank(remotePath, PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "FTP/FTPS 远端路径不能为空");
        FTPClient client = "FTPS".equals(fetchMode) ? new FTPSClient(true) : new FTPClient();
        client.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
        client.setDataTimeout(Duration.ofMillis(DATA_TIMEOUT_MILLIS));
        try {
            client.connect(endpoint.host(), endpoint.port());
            Require.isTrue(FTPReply.isPositiveCompletion(client.getReplyCode()),
                    PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), fetchMode + " 服务器连接失败");
            login(client, source.getCredentialRef());
            client.enterLocalPassiveMode();
            client.setFileType(FTP.BINARY_FILE_TYPE);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            boolean retrieved = client.retrieveFile(remotePath, output);
            Require.isTrue(retrieved,
                    PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), fetchMode + " 账单文件读取失败：" + remotePath);
            String body = output.toString(StandardCharsets.UTF_8);
            return new RemoteBillFile(fileName(remotePath), body);
        } catch (IOException ex) {
            throw new IllegalArgumentException(fetchMode + " 账单获取失败：" + ex.getMessage(), ex);
        } finally {
            disconnect(client);
        }
    }

    private void login(FTPClient client, String credentialRef) throws IOException {
        String ref = PaymentContextSupport.trimToNull(credentialRef);
        if (ref == null) {
            Require.isTrue(client.login("anonymous", "anonymous@localhost"),
                    PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "FTP/FTPS 匿名登录失败");
            return;
        }
        Optional<PaymentChannelBillCredentialProvider.Credential> credential = resolveCredential(ref);
        Require.isTrue(credential.isPresent(),
                PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "未找到 FTP/FTPS 认证配置引用：" + ref);
        PaymentChannelBillCredentialProvider.Credential value = credential.get();
        Require.notBlank(value.username(), PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "FTP/FTPS 认证用户名不能为空");
        Require.isTrue(client.login(value.username(), value.password() == null ? "" : value.password()),
                PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "FTP/FTPS 登录失败");
    }

    private Endpoint parseEndpoint(String endpoint, int defaultPort) {
        String value = PaymentContextSupport.trimToNull(endpoint);
        Require.notBlank(value, PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "FTP/FTPS 服务器地址不能为空");
        Require.isTrue(!value.contains("@"),
                PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "FTP/FTPS 服务器地址不能包含账号密码");
        String normalized = value
                .replaceFirst("(?i)^ftp://", "")
                .replaceFirst("(?i)^ftps://", "");
        int slashIndex = normalized.indexOf('/');
        if (slashIndex >= 0) {
            normalized = normalized.substring(0, slashIndex);
        }
        String host = normalized;
        int port = defaultPort;
        int colonIndex = normalized.lastIndexOf(':');
        if (colonIndex > 0 && colonIndex < normalized.length() - 1) {
            host = normalized.substring(0, colonIndex);
            try {
                port = Integer.parseInt(normalized.substring(colonIndex + 1));
            } catch (NumberFormatException ex) {
                Require.isTrue(false, PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "FTP/FTPS 服务器端口无效");
            }
        }
        Require.isTrue(StringUtils.hasText(host),
                PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "FTP/FTPS 服务器地址不能为空");
        Require.isTrue(port > 0 && port <= 65535,
                PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "FTP/FTPS 服务器端口无效");
        return new Endpoint(host, port);
    }

    private Optional<PaymentChannelBillCredentialProvider.Credential> resolveCredential(String credentialRef) {
        for (PaymentChannelBillCredentialProvider provider : credentialProviders) {
            Optional<PaymentChannelBillCredentialProvider.Credential> credential = provider.resolve(credentialRef);
            if (credential.isPresent()) {
                return credential;
            }
        }
        return Optional.empty();
    }

    private String fileName(String remotePath) {
        int index = remotePath.lastIndexOf('/');
        String name = index >= 0 ? remotePath.substring(index + 1) : remotePath;
        return StringUtils.hasText(name) ? name : "channel-bill.json";
    }

    private void disconnect(FTPClient client) {
        if (!client.isConnected()) {
            return;
        }
        try {
            client.logout();
        } catch (IOException ex) {
            log.debug("FTP/FTPS logout failed after channel bill fetch", ex);
        }
        try {
            client.disconnect();
        } catch (IOException ex) {
            log.debug("FTP/FTPS disconnect failed after channel bill fetch", ex);
        }
    }

    private record Endpoint(String host, int port) {
    }

    public record RemoteBillFile(String fileName, String body) {
    }
}
