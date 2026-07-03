package io.mango.link.core.service;

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

import java.util.List;

public interface ILinkAdminService {

    PageResult<LinkCategoryVO> pageCategories(LinkCategoryPageQuery query);

    List<LinkCategoryVO> listCategories(LinkCategoryQuery query);

    Long createCategory(CreateLinkCategoryCommand command);

    boolean updateCategory(UpdateLinkCategoryCommand command);

    boolean updateCategoryStatus(UpdateLinkCategoryStatusCommand command);

    boolean deleteCategory(Long id);

    PageResult<LinkItemVO> pageItems(LinkItemPageQuery query);

    Long createItem(CreateLinkItemCommand command);

    boolean updateItem(UpdateLinkItemCommand command);

    boolean updateItemStatus(UpdateLinkItemStatusCommand command);

    boolean deleteItem(Long id);
}
