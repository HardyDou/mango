package io.mango.infra.fileproc.fixture;

import io.mango.common.contract.LocalCapabilityContract;

import java.io.InputStream;

@LocalCapabilityContract
public interface LocalFileProcessorApi {

    byte[] process(InputStream inputStream);
}
