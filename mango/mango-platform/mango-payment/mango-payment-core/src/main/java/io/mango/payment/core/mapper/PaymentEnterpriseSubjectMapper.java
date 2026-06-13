package io.mango.payment.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.payment.core.entity.PaymentEnterpriseSubject;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PaymentEnterpriseSubjectMapper extends BaseMapper<PaymentEnterpriseSubject> {

    long countDeleteRelations(@Param("tenantId") Long tenantId, @Param("subjectId") Long subjectId);
}
