package io.mango.payment.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.payment.core.entity.PayRefundOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PayRefundOrderMapper extends BaseMapper<PayRefundOrder> {

    @Select("""
            SELECT * FROM mango_pay_refund_order
            WHERE biz_order_id = #{bizOrderId}
              AND idempotency_key = #{idempotencyKey}
            LIMIT 1
            """)
    PayRefundOrder selectByIdempotencyKey(@Param("bizOrderId") Long bizOrderId,
                                          @Param("idempotencyKey") String idempotencyKey);

    @Select("""
            SELECT * FROM mango_pay_refund_order
            WHERE biz_order_id = #{bizOrderId}
              AND merchant_refund_no = #{merchantRefundNo}
            LIMIT 1
            """)
    PayRefundOrder selectByMerchantRefundNo(@Param("bizOrderId") Long bizOrderId,
                                            @Param("merchantRefundNo") String merchantRefundNo);

    @Update("""
            UPDATE mango_pay_refund_order
            SET status = #{status}, channel_refund_no = #{channelRefundNo}, update_time = NOW()
            WHERE id = #{refundOrderId}
            """)
    int updateStatus(@Param("refundOrderId") Long refundOrderId,
                     @Param("status") String status,
                     @Param("channelRefundNo") String channelRefundNo);
}
