package io.mango.link.core.service;

import io.mango.link.api.query.LinkPublicItemQuery;
import io.mango.link.api.vo.LinkPublicItemVO;

import java.util.List;

public interface ILinkOpenService {

    List<LinkPublicItemVO> listPublicItems(LinkPublicItemQuery query);

    String resolveRedirectUrl(Long id, String source, String clientIp, String userAgent, String referer);

    String resolveJumpUrl(String url, String visitorId, String source, String extraParams,
                          String clientIp, String userAgent, String referer);
}
