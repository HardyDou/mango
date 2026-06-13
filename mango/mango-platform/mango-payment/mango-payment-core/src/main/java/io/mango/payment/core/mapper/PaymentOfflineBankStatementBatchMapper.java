package io.mango.payment.core.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.payment.api.vo.PaymentOfflineBankStatementBatchVO;
import io.mango.payment.core.entity.PaymentOfflineBankStatementBatchEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PaymentOfflineBankStatementBatchMapper extends BaseMapper<PaymentOfflineBankStatementBatchEntity> {

    @InterceptorIgnore(tenantLine = "true")
    long countBatches(
            @Param("tenantId") Long tenantId,
            @Param("keyword") String keyword,
            @Param("statusCode") String statusCode);

    @InterceptorIgnore(tenantLine = "true")
    List<PaymentOfflineBankStatementBatchVO> selectBatchPage(
            @Param("tenantId") Long tenantId,
            @Param("keyword") String keyword,
            @Param("statusCode") String statusCode,
            @Param("limit") long limit,
            @Param("offset") long offset);

    @InterceptorIgnore(tenantLine = "true")
    PaymentOfflineBankStatementBatchVO selectBatchDetail(
            @Param("tenantId") Long tenantId,
            @Param("id") Long id);

    @InterceptorIgnore(tenantLine = "true")
    long countImportedFile(
            @Param("tenantId") Long tenantId,
            @Param("fileDigest") String fileDigest);

    @InterceptorIgnore(tenantLine = "true")
    int refreshSummary(@Param("tenantId") Long tenantId, @Param("id") Long id);
}
