package io.mango.guarantee.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.guarantee.api.query.GuaranteeCaseQuery;
import io.mango.guarantee.api.vo.GuaranteeCaseVO;
import io.mango.guarantee.core.entity.GuaranteeCase;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 保函业务单 Mapper。
 */
@Mapper
public interface GuaranteeCaseMapper extends BaseMapper<GuaranteeCase> {

    @Select("""
            <script>
            SELECT
                c.id AS caseId,
                c.case_no AS caseNo,
                c.source_tenant_id AS sourceTenantId,
                t.tenant_name AS sourceTenantName,
                c.title,
                c.applicant_name AS applicantName,
                c.beneficiary_name AS beneficiaryName,
                c.guarantee_type AS guaranteeType,
                c.amount,
                c.currency,
                c.expected_issue_date AS expectedIssueDate,
                c.status,
                c.remark,
                c.create_time AS createTime,
                c.update_time AS updateTime
            FROM guarantee_case c
            LEFT JOIN sys_tenant t ON t.id = c.source_tenant_id
            WHERE c.del_flag = 0
              AND (
                    c.source_tenant_id = #{tenantId}
                    OR EXISTS (
                        SELECT 1
                        FROM guarantee_case_participant p
                        WHERE p.case_id = c.id
                          AND p.participant_tenant_id = #{tenantId}
                          AND p.status = 1
                          AND p.del_flag = 0
                    )
                  )
            <if test="query != null and query.title != null and query.title != ''">
              AND c.title LIKE CONCAT('%', #{query.title}, '%')
            </if>
            <if test="query != null and query.applicantName != null and query.applicantName != ''">
              AND c.applicant_name LIKE CONCAT('%', #{query.applicantName}, '%')
            </if>
            <if test="query != null and query.guaranteeType != null and query.guaranteeType != ''">
              AND c.guarantee_type = #{query.guaranteeType}
            </if>
            <if test="query != null and query.status != null">
              AND c.status = #{query.status}
            </if>
            ORDER BY c.create_time DESC, c.id DESC
            </script>
            """)
    @InterceptorIgnore(tenantLine = "true")
    List<GuaranteeCaseVO> selectVisiblePage(Page<GuaranteeCaseVO> page,
                                            @Param("tenantId") Long tenantId,
                                            @Param("query") GuaranteeCaseQuery query);

    @Select("""
            SELECT
                c.id AS caseId,
                c.case_no AS caseNo,
                c.source_tenant_id AS sourceTenantId,
                t.tenant_name AS sourceTenantName,
                c.title,
                c.applicant_name AS applicantName,
                c.beneficiary_name AS beneficiaryName,
                c.guarantee_type AS guaranteeType,
                c.amount,
                c.currency,
                c.expected_issue_date AS expectedIssueDate,
                c.status,
                c.remark,
                c.create_time AS createTime,
                c.update_time AS updateTime
            FROM guarantee_case c
            LEFT JOIN sys_tenant t ON t.id = c.source_tenant_id
            WHERE c.id = #{caseId}
              AND c.del_flag = 0
              AND (
                    c.source_tenant_id = #{tenantId}
                    OR EXISTS (
                        SELECT 1
                        FROM guarantee_case_participant p
                        WHERE p.case_id = c.id
                          AND p.participant_tenant_id = #{tenantId}
                          AND p.status = 1
                          AND p.del_flag = 0
                    )
                  )
            LIMIT 1
            """)
    @InterceptorIgnore(tenantLine = "true")
    GuaranteeCaseVO selectVisibleById(@Param("caseId") Long caseId, @Param("tenantId") Long tenantId);

    @Update("""
            UPDATE guarantee_case
            SET del_flag = 1,
                update_time = NOW()
            WHERE id = #{caseId}
              AND source_tenant_id = #{tenantId}
              AND del_flag = 0
            """)
    @InterceptorIgnore(tenantLine = "true")
    int softDeleteSourceCase(@Param("caseId") Long caseId, @Param("tenantId") Long tenantId);
}
