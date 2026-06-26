package io.mango.notice.core.service;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.identity.api.IdentityUserApi;
import io.mango.identity.api.enums.IdentityUserTargetType;
import io.mango.identity.api.query.IdentityUserPageQuery;
import io.mango.identity.api.query.IdentityUserTargetQuery;
import io.mango.identity.api.vo.IdentityUserInfo;
import io.mango.identity.api.vo.IdentityUserVO;
import io.mango.notice.api.command.NoticeRecipientCommand;
import io.mango.notice.api.command.NoticeRecipientTargetCommand;
import io.mango.notice.api.command.SendNoticeCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class NoticeRecipientResolver {

    private static final int ALL_USER_PAGE_SIZE = 200;

    private final IdentityUserApi identityUserApi;

    public List<NoticeRecipientCommand> resolveRecipients(SendNoticeCommand command) {
        Map<Long, NoticeRecipientCommand> userRecipients = new LinkedHashMap<>();
        List<NoticeRecipientCommand> externalRecipients = new ArrayList<>();
        if (command.getRecipients() != null) {
            for (NoticeRecipientCommand recipient : command.getRecipients()) {
                if (recipient.getUserId() == null) {
                    externalRecipients.add(recipient);
                } else {
                    enrichRecipientFromUser(recipient);
                    userRecipients.putIfAbsent(recipient.getUserId(), recipient);
                }
            }
        }
        receiverIds(command).forEach(userId -> {
            NoticeRecipientCommand recipient = new NoticeRecipientCommand();
            recipient.setUserId(userId);
            enrichRecipientFromUser(recipient);
            userRecipients.putIfAbsent(userId, recipient);
        });
        resolveRecipientTargets(command.getRecipientTargets()).forEach(recipient ->
                userRecipients.putIfAbsent(recipient.getUserId(), recipient));
        List<NoticeRecipientCommand> recipients = new ArrayList<>(externalRecipients);
        recipients.addAll(userRecipients.values());
        return recipients;
    }

    public List<NoticeRecipientCommand> resolveRecipientTargets(List<NoticeRecipientTargetCommand> targets) {
        if (targets == null || targets.isEmpty() || identityUserApi == null) {
            return List.of();
        }
        return targets.stream()
                .filter(target -> target.getTargetType() != null && target.getTargetId() != null)
                .flatMap(target -> listUsersByTarget(target).stream())
                .collect(Collectors.toMap(NoticeRecipientCommand::getUserId, Function.identity(), (left, right) -> left,
                        LinkedHashMap::new))
                .values()
                .stream()
                .toList();
    }

    public List<NoticeRecipientCommand> listAllEnabledUsers() {
        if (identityUserApi == null) {
            return List.of();
        }
        Map<Long, NoticeRecipientCommand> users = new LinkedHashMap<>();
        long pageNum = 1;
        while (true) {
            IdentityUserPageQuery query = new IdentityUserPageQuery();
            query.setPage(pageNum);
            query.setSize(ALL_USER_PAGE_SIZE);
            query.setStatus(1);
            R<PageResult<IdentityUserVO>> response = identityUserApi.page(query);
            if (response == null || !response.isSuccess() || response.getData() == null) {
                break;
            }
            PageResult<IdentityUserVO> page = response.getData();
            List<IdentityUserVO> list = page.getList() == null ? List.of() : page.getList();
            list.stream()
                    .filter(user -> user.getUserId() != null)
                    .map(this::toRecipient)
                    .forEach(recipient -> users.putIfAbsent(recipient.getUserId(), recipient));
            long total = page.getTotal();
            long loaded = pageNum * ALL_USER_PAGE_SIZE;
            if (list.isEmpty() || loaded >= total) {
                break;
            }
            pageNum++;
        }
        return new ArrayList<>(users.values());
    }

    public void enrichRecipientFromUser(NoticeRecipientCommand recipient) {
        if (recipient.getUserId() == null || identityUserApi == null) {
            return;
        }
        R<IdentityUserInfo> response = identityUserApi.getUserInfoById(recipient.getUserId());
        if (response == null || !response.isSuccess() || response.getData() == null) {
            return;
        }
        IdentityUserInfo user = response.getData();
        if (!StringUtils.hasText(recipient.getRecipientName())) {
            recipient.setRecipientName(firstText(user.getNickname(), user.getUsername()));
        }
        if (!StringUtils.hasText(recipient.getMobile())) {
            recipient.setMobile(user.getPhone());
        }
        if (!StringUtils.hasText(recipient.getEmail())) {
            recipient.setEmail(user.getEmail());
        }
    }

    private List<Long> receiverIds(SendNoticeCommand command) {
        List<Long> ids = new ArrayList<>();
        if (command.getUserId() != null) {
            ids.add(command.getUserId());
        }
        if (command.getUserIds() != null) {
            ids.addAll(command.getUserIds());
        }
        return ids.stream().filter(id -> id != null).distinct().toList();
    }

    private List<NoticeRecipientCommand> listUsersByTarget(NoticeRecipientTargetCommand target) {
        IdentityUserTargetQuery query = new IdentityUserTargetQuery();
        query.setTargetType(IdentityUserTargetType.valueOf(target.getTargetType().name()));
        query.setTargetId(target.getTargetId());
        query.setStatus(1);
        R<List<IdentityUserInfo>> response = identityUserApi.listUserInfosByTarget(query);
        if (response == null || !response.isSuccess() || response.getData() == null) {
            return List.of();
        }
        return response.getData().stream()
                .filter(user -> user.getUserId() != null)
                .map(this::toRecipient)
                .toList();
    }

    private NoticeRecipientCommand toRecipient(IdentityUserInfo user) {
        NoticeRecipientCommand recipient = new NoticeRecipientCommand();
        recipient.setUserId(user.getUserId());
        recipient.setRecipientName(firstText(user.getNickname(), user.getUsername()));
        recipient.setMobile(user.getPhone());
        recipient.setEmail(user.getEmail());
        return recipient;
    }

    private NoticeRecipientCommand toRecipient(IdentityUserVO user) {
        NoticeRecipientCommand recipient = new NoticeRecipientCommand();
        recipient.setUserId(user.getUserId());
        recipient.setRecipientName(firstText(user.getNickname(), user.getUsername()));
        recipient.setMobile(user.getPhone());
        recipient.setEmail(user.getEmail());
        return recipient;
    }

    private String firstText(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }
}
