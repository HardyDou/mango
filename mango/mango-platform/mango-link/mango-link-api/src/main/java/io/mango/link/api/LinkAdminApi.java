package io.mango.link.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.link.api.command.CreateLinkCategoryCommand;
import io.mango.link.api.command.CreateLinkItemCommand;
import io.mango.link.api.command.UpdateLinkCategoryCommand;
import io.mango.link.api.command.UpdateLinkCategoryStatusCommand;
import io.mango.link.api.command.UpdateLinkItemCommand;
import io.mango.link.api.command.UpdateLinkItemStatusCommand;
import io.mango.link.api.query.LinkCategoryPageQuery;
import io.mango.link.api.query.LinkCategoryQuery;
import io.mango.link.api.query.LinkItemPageQuery;
import io.mango.link.api.vo.LinkCategoryVO;
import io.mango.link.api.vo.LinkItemVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * 网址后台管理 API 契约。
 */
@Validated
public interface LinkAdminApi {

    R<PageResult<LinkCategoryVO>> pageCategories(@Valid LinkCategoryPageQuery query);

    R<List<LinkCategoryVO>> listCategories(@Valid LinkCategoryQuery query);

    R<Long> createCategory(@Valid CreateLinkCategoryCommand command);

    R<Boolean> updateCategory(@Valid UpdateLinkCategoryCommand command);

    R<Boolean> enableCategory(@NotNull(message = "分类 ID 不能为空") Long id);

    R<Boolean> disableCategory(@NotNull(message = "分类 ID 不能为空") Long id);

    R<Boolean> updateCategoryStatus(@Valid UpdateLinkCategoryStatusCommand command);

    R<Boolean> deleteCategory(@NotNull(message = "分类 ID 不能为空") Long id);

    R<PageResult<LinkItemVO>> pageItems(@Valid LinkItemPageQuery query);

    R<Long> createItem(@Valid CreateLinkItemCommand command);

    R<Boolean> updateItem(@Valid UpdateLinkItemCommand command);

    R<Boolean> enableItem(@NotNull(message = "网址 ID 不能为空") Long id);

    R<Boolean> disableItem(@NotNull(message = "网址 ID 不能为空") Long id);

    R<Boolean> updateItemStatus(@Valid UpdateLinkItemStatusCommand command);

    R<Boolean> deleteItem(@NotNull(message = "网址 ID 不能为空") Long id);
}
