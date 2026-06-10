package io.mango.notice.channel.wecom;

public record WecomDepartment(
        Long id,
        String name,
        Long parentId,
        Integer order) {
}
