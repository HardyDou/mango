package io.mango.link.core.service;

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

import java.util.List;

public interface ILinkUserService {

    List<LinkNavigationItemVO> listCompanyItems(LinkCompanyItemQuery query);

    List<LinkCategoryVO> listPersonalCategories();

    Long createPersonalCategory(CreateLinkPersonalCategoryCommand command);

    boolean createFavorite(CreateLinkFavoriteCommand command);

    boolean deleteFavorite(DeleteLinkFavoriteCommand command);

    List<LinkFavoriteVO> listFavorites(LinkFavoriteQuery query);

    PageResult<LinkPersonalItemVO> pagePersonalItems(LinkPersonalItemPageQuery query);

    Long createPersonalItem(CreateLinkPersonalItemCommand command);

    boolean updatePersonalItem(UpdateLinkPersonalItemCommand command);

    boolean deletePersonalItem(Long id);
}
