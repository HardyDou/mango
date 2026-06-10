package io.mango.payment.core.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.payment.api.vo.PaymentOfflineBankStatementItemVO;
import io.mango.payment.core.entity.PaymentOfflineBankStatementItemEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface PaymentOfflineBankStatementItemMapper extends BaseMapper<PaymentOfflineBankStatementItemEntity> {

    @InterceptorIgnore(tenantLine = "true")
    List<PaymentOfflineBankStatementItemVO> selectItemsByBatch(
            @Param("tenantId") Long tenantId,
            @Param("batchId") Long batchId);

    @InterceptorIgnore(tenantLine = "true")
    PaymentOfflineBankStatementItemEntity selectEntityForUpdate(
            @Param("tenantId") Long tenantId,
            @Param("id") Long id);

    @InterceptorIgnore(tenantLine = "true")
    long countExistingStatement(
            @Param("tenantId") Long tenantId,
            @Param("bankAccountNoMask") String bankAccountNoMask,
            @Param("bankStatementNo") String bankStatementNo,
            @Param("tradeDate") LocalDate tradeDate);

    @InterceptorIgnore(tenantLine = "true")
    int markConfirmed(
            @Param("tenantId") Long tenantId,
            @Param("id") Long id,
            @Param("confirmedTime") LocalDateTime confirmedTime,
            @Param("confirmedBy") Long confirmedBy,
            @Param("confirmedByName") String confirmedByName,
            @Param("confirmRemark") String confirmRemark);
}
