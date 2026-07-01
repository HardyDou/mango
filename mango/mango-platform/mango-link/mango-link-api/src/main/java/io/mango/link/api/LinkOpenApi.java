package io.mango.link.api;

import io.mango.common.result.R;
import io.mango.link.api.query.LinkPublicItemQuery;
import io.mango.link.api.vo.LinkPublicItemVO;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * 网址公开 Open API 契约。
 */
@Validated
public interface LinkOpenApi {

    R<List<LinkPublicItemVO>> listPublicItems(@Valid LinkPublicItemQuery query);
}
