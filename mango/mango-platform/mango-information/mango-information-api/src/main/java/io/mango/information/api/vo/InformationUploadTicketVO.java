package io.mango.information.api.vo;

import lombok.Data;

/** 公开上传票据。 */
@Data
public class InformationUploadTicketVO {
    /** 票据内容，上传时通过 X-Mango-Upload-Ticket 头带回。 */
    private String ticket;
    /** 有效期，单位秒。 */
    private long expiresIn;
}
