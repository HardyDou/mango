package io.mango.payment.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.payment.core.entity.PayBizOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PayBizOrderMapper extends BaseMapper<PayBizOrder> {

    @Select("""
            SELECT * FROM mango_pay_biz_order
            WHERE app_code = #{appCode}
              AND merchant_order_no = #{merchantOrderNo}
              AND tenant_id = #{tenantId}
            LIMIT 1
            """)
    PayBizOrder selectByMerchantOrder(@Param("appCode") String appCode,
                                      @Param("merchantOrderNo") String merchantOrderNo,
                                      @Param("tenantId") Long tenantId);

    @Update("""
            UPDATE mango_pay_biz_order
            SET status = #{status}, update_time = NOW()
            WHERE id = #{bizOrderId}
            """)
    int updateStatus(@Param("bizOrderId") Long bizOrderId, @Param("status") String status);

    @Update("""
            UPDATE mango_pay_biz_order
            SET refunded_amount = refunded_amount + #{refundAmount},
                status = #{status},
                update_time = NOW()
            WHERE id = #{bizOrderId}
              AND refunded_amount + #{refundAmount} <= amount
            """)
    int addRefundedAmount(@Param("bizOrderId") Long bizOrderId,
                          @Param("refundAmount") Long refundAmount,
                          @Param("status") String status);
}
