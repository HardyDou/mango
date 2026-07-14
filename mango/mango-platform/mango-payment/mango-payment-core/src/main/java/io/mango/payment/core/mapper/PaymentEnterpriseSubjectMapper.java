package io.mango.payment.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.payment.core.entity.PaymentEnterpriseSubjectEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PaymentEnterpriseSubjectMapper extends BaseMapper<PaymentEnterpriseSubjectEntity> {

    long countDeleteRelations(@Param("tenantId") String tenantId, @Param("subjectId") Long subjectId);
}
