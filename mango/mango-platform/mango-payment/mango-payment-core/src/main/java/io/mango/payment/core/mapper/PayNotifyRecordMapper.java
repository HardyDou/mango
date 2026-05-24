package io.mango.payment.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.payment.core.entity.PayNotifyRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PayNotifyRecordMapper extends BaseMapper<PayNotifyRecord> {

    @Select("""
            SELECT * FROM mango_pay_notify_record
            WHERE notify_event_id = #{notifyEventId}
            LIMIT 1
            """)
    PayNotifyRecord selectByNotifyEventId(@Param("notifyEventId") String notifyEventId);
}
