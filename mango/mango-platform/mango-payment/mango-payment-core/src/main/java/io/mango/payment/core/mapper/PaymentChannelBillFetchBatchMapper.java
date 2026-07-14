package io.mango.payment.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.payment.core.model.projection.PaymentChannelBillFetchBatchProjection;
import io.mango.payment.core.entity.PaymentChannelBillFetchBatchEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PaymentChannelBillFetchBatchMapper extends BaseMapper<PaymentChannelBillFetchBatchEntity> {

    long countFetchBatches(
            @Param("tenantId") String tenantId,
            @Param("keyword") String keyword);

    List<PaymentChannelBillFetchBatchProjection> selectFetchBatchPage(
            @Param("tenantId") String tenantId,
            @Param("keyword") String keyword,
            @Param("limit") long limit,
            @Param("offset") long offset);

    long countSuccessfulFetch(
            @Param("tenantId") String tenantId,
            @Param("sourceId") Long sourceId,
            @Param("billDate") java.time.LocalDate billDate);
}
