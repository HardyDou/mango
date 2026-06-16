package io.mango.gridlayout.core.service;

import io.mango.gridlayout.api.command.SaveGridLayoutPersonalCommand;
import io.mango.gridlayout.api.query.GridLayoutPersonalQuery;
import io.mango.gridlayout.api.vo.GridLayoutPersonalVO;

public interface IGridLayoutPersonalService {

    GridLayoutPersonalVO getPersonal(GridLayoutPersonalQuery query);

    GridLayoutPersonalVO savePersonal(SaveGridLayoutPersonalCommand command);

    boolean deletePersonal(GridLayoutPersonalQuery query);
}
