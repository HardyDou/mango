package io.mango.payment.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.payment.core.entity.PayPaymentOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PayPaymentOrderMapper extends BaseMapper<PayPaymentOrder> {

    @Select("""
            SELECT * FROM mango_pay_payment_order
            WHERE biz_order_id = #{bizOrderId}
              AND idempotency_key = #{idempotencyKey}
            LIMIT 1
            """)
    PayPaymentOrder selectByIdempotencyKey(@Param("bizOrderId") Long bizOrderId,
                                           @Param("idempotencyKey") String idempotencyKey);

    @Select("""
            SELECT * FROM mango_pay_payment_order
            WHERE biz_order_id = #{bizOrderId}
              AND status = 'PROCESSING'
            LIMIT 1
            """)
    PayPaymentOrder selectProcessingByBizOrderId(@Param("bizOrderId") Long bizOrderId);

    @Select("""
            SELECT * FROM mango_pay_payment_order
            WHERE biz_order_id = #{bizOrderId}
              AND status = 'SUCCESS'
            ORDER BY create_time DESC
            LIMIT 1
            """)
    PayPaymentOrder selectSuccessByBizOrderId(@Param("bizOrderId") Long bizOrderId);

    @Update("""
            UPDATE mango_pay_payment_order
            SET status = #{status}, channel_order_no = #{channelOrderNo}, update_time = NOW()
            WHERE id = #{paymentOrderId}
            """)
    int updateStatus(@Param("paymentOrderId") Long paymentOrderId,
                     @Param("status") String status,
                     @Param("channelOrderNo") String channelOrderNo);

    @Update("""
            UPDATE mango_pay_payment_order
            SET status = 'CLOSED', update_time = NOW()
            WHERE biz_order_id = #{bizOrderId}
              AND status IN ('CREATED', 'PROCESSING')
              AND id <> #{successPaymentOrderId}
            """)
    int closeOtherProcessing(@Param("bizOrderId") Long bizOrderId,
                             @Param("successPaymentOrderId") Long successPaymentOrderId);
}
