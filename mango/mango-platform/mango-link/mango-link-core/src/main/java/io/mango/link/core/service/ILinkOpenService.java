package io.mango.link.core.service;

import io.mango.link.api.query.LinkPublicItemQuery;
import io.mango.link.api.vo.LinkPublicItemVO;

import java.util.List;

public interface ILinkOpenService {

    List<LinkPublicItemVO> listPublicItems(LinkPublicItemQuery query);

    List<LinkPublicItemVO> listVisibleItems(LinkPublicItemQuery query);

    String resolveRedirectUrl(LinkRedirectContext context);

    String resolveJumpUrl(LinkJumpContext context);
}
