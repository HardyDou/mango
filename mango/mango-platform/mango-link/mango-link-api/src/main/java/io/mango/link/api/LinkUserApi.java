package io.mango.link.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.link.api.command.CreateLinkFavoriteCommand;
import io.mango.link.api.command.CreateLinkPersonalCategoryCommand;
import io.mango.link.api.command.CreateLinkPersonalItemCommand;
import io.mango.link.api.command.DeleteLinkFavoriteCommand;
import io.mango.link.api.command.UpdateLinkPersonalItemCommand;
import io.mango.link.api.query.LinkCompanyItemQuery;
import io.mango.link.api.query.LinkFavoriteQuery;
import io.mango.link.api.query.LinkPersonalItemPageQuery;
import io.mango.link.api.vo.LinkFavoriteVO;
import io.mango.link.api.vo.LinkCategoryVO;
import io.mango.link.api.vo.LinkNavigationItemVO;
import io.mango.link.api.vo.LinkPersonalItemVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * 网址用户侧 API 契约。
 */
@Validated
public interface LinkUserApi {

    R<List<LinkNavigationItemVO>> listCompanyItems(@Valid LinkCompanyItemQuery query);

    R<List<LinkCategoryVO>> listPersonalCategories();

    R<Long> createPersonalCategory(@Valid CreateLinkPersonalCategoryCommand command);

    R<Boolean> createFavorite(@Valid CreateLinkFavoriteCommand command);

    R<Boolean> deleteFavorite(@Valid DeleteLinkFavoriteCommand command);

    R<List<LinkFavoriteVO>> listFavorites(@Valid LinkFavoriteQuery query);

    R<PageResult<LinkPersonalItemVO>> pagePersonalItems(@Valid LinkPersonalItemPageQuery query);

    R<Long> createPersonalItem(@Valid CreateLinkPersonalItemCommand command);

    R<Boolean> updatePersonalItem(@Valid UpdateLinkPersonalItemCommand command);

    R<Boolean> deletePersonalItem(@NotNull(message = "网址 ID 不能为空") Long id);
}
