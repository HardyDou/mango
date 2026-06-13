package io.mango.payment.api.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class PaymentChannelBillSourceVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long contractId;
    private String contractName;
    private Long channelId;
    private String channelName;
    private Long subjectId;
    private String subjectName;
    private String merchantNo;
    private String channelCode;
    private String fetchMode;
    private String fetchModeName;
    private String endpoint;
    private String remotePath;
    private String credentialRef;
    private String pageMode;
    private Integer enabled;
    private String enabledName;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
