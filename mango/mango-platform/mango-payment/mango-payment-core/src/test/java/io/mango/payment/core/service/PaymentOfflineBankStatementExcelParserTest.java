package io.mango.payment.core.service;

import io.mango.payment.api.PaymentCode;
import io.mango.payment.api.enums.PaymentOfflineBankStatementMatchStatusEnum;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentOfflineBankStatementExcelParserTest {

    private final PaymentOfflineChannelService service = new PaymentOfflineChannelService(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            new PaymentSensitiveValueService(null),
            null,
            null);

    @Test
    @DisplayName("parseBankStatementRows should parse backend uploaded Excel into real statement rows")
    void parseBankStatementRows_backendExcel_parsesRows() throws Exception {
        byte[] content = workbook(
                List.of("银行流水号", "交易时间", "收入金额", "备注", "收款账号", "开户行", "付款户名", "付款账号"),
                List.of("BNK202606080001", "2026-06-08 10:20:30", "1,234.56", "转账备注 A7K9Q2", "6222000012345678901", "招商银行", "张三", "6217000099998888777"));

        List<?> rows = parseRows(content);
        Object row = rows.get(0);

        assertThat(rows).hasSize(1);
        assertThat(call(row, "bankStatementNo")).isEqualTo("BNK202606080001");
        assertThat(call(row, "tradeTime")).isEqualTo(LocalDateTime.of(2026, 6, 8, 10, 20, 30));
        assertThat(call(row, "amount")).isEqualTo(123456L);
        assertThat(call(row, "reconciliationCode")).isEqualTo("A7K9Q2");
        assertThat(call(row, "bankAccountNoMask")).isEqualTo("6222****8901");
        assertThat(call(row, "counterpartyAccountNoMask")).isEqualTo("6217****8777");
    }

    @Test
    @DisplayName("match status enum should require finance confirmation before success projection")
    void matchStatusEnum_matchedRequiresFinanceConfirmation() {
        assertThat(PaymentOfflineBankStatementMatchStatusEnum.MATCHED_PENDING_CONFIRM.getCode())
                .isEqualTo("MATCHED_PENDING_CONFIRM");
        assertThat(PaymentOfflineBankStatementMatchStatusEnum.CONFIRMED.getCode())
                .isEqualTo("CONFIRMED");
    }

    @Test
    @DisplayName("parseBankStatementRows should reject Excel without required statement columns")
    void parseBankStatementRows_missingRequiredColumns_rejects() {
        byte[] content = workbook(
                List.of("交易时间", "收入金额", "备注"),
                List.of("2026-06-08 10:20:30", "10.00", "A7K9Q2"));

        assertThatThrownBy(() -> parseRows(content))
                .hasMessageContaining("缺少 银行流水号");
    }

    private List<?> parseRows(byte[] content) throws Exception {
        Method method = PaymentOfflineChannelService.class.getDeclaredMethod("parseBankStatementRows", byte[].class);
        method.setAccessible(true);
        try {
            return (List<?>) method.invoke(service, content);
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw ex;
        }
    }

    private Object call(Object target, String methodName) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        return method.invoke(target);
    }

    private byte[] workbook(List<String> headers, List<String> values) {
        try (var workbook = new XSSFWorkbook();
             var output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("bank");
            var header = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                header.createCell(i).setCellValue(headers.get(i));
            }
            var row = sheet.createRow(1);
            for (int i = 0; i < values.size(); i++) {
                row.createCell(i).setCellValue(values.get(i));
            }
            workbook.write(output);
            return output.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to create Excel test file", ex);
        }
    }
}
