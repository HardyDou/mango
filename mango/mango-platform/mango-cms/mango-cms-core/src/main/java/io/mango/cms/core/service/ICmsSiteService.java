package io.mango.cms.core.service;

import io.mango.cms.api.CmsSiteApi;
import io.mango.cms.api.query.SiteResolveQuery;
import io.mango.file.api.vo.FileDownloadVO;

public interface ICmsSiteService extends CmsSiteApi {

    FileDownloadVO publicFile(Long id, SiteResolveQuery query);
}
