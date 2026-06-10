package io.mango.payment.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import io.mango.payment.api.vo.PaymentDifferenceVO;
import io.mango.payment.core.entity.PaymentDifferenceEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface PaymentDifferenceMapper extends BaseMapper<PaymentDifferenceEntity> {

    @InterceptorIgnore(tenantLine = "true")
    long countDifferences(
            @Param("tenantId") Long tenantId,
            @Param("keyword") String keyword,
            @Param("statusCode") String statusCode);

    @InterceptorIgnore(tenantLine = "true")
    List<PaymentDifferenceVO> selectDifferencePage(
            @Param("tenantId") Long tenantId,
            @Param("keyword") String keyword,
            @Param("statusCode") String statusCode,
            @Param("limit") long limit,
            @Param("offset") long offset);

    @InterceptorIgnore(tenantLine = "true")
    PaymentDifferenceVO selectDifferenceDetail(
            @Param("tenantId") Long tenantId,
            @Param("id") Long id);

    @InterceptorIgnore(tenantLine = "true")
    int handleDifference(
            @Param("tenantId") Long tenantId,
            @Param("id") Long id,
            @Param("processStatus") String processStatus,
            @Param("processAction") String processAction,
            @Param("processReason") String processReason,
            @Param("processResult") String processResult,
            @Param("processEvidence") String processEvidence,
            @Param("adjustFlowId") Long adjustFlowId,
            @Param("adjustFlowNo") String adjustFlowNo,
            @Param("processorId") Long processorId,
            @Param("processorName") String processorName,
            @Param("processTime") LocalDateTime processTime);
}
