package io.mango.information.core.service;

import io.mango.information.api.vo.InformationFileVO;
import io.mango.information.api.vo.InformationUploadTicketVO;

import java.io.InputStream;

/**
 * 信息采集公开上传能力。
 * <p>
 * 托管表单是匿名入口，不能直接调用需要登录的文件上传接口；
 * 该能力由信息采集模块提供表单级公开入口，文件最终仍由 mango-file 保存。
 */
public interface IInformationFileService {

    /**
     * 为公开表单签发短效上传票据；上传接口必须携带有效票据。
     */
    InformationUploadTicketVO issueTicket(String formCode);

    /**
     * 按表单保存上传文件。
     *
     * @param formCode 表单编码
     * @param fileName 原始文件名
     * @param contentType 内容类型
     * @param fileSize 文件大小
     * @param inputStream 文件流
     * @param ticket 上传票据
     * @return 文件信息
     */
    InformationFileVO upload(String formCode, String fileName, String contentType, long fileSize,
            InputStream inputStream, String ticket);

    /**
     * 查询当前表单可访问的文件信息。
     */
    InformationFileVO detail(String formCode, Long id);
}
