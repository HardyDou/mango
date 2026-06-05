package io.mango.notice.channel.wecom;

import java.util.List;

public record WecomDirectoryUser(
        String userId,
        String name,
        List<Long> departments,
        String position,
        String mobile,
        String gender,
        String email,
        String bizMail,
        String avatar,
        String alias,
        Integer status) {
}
