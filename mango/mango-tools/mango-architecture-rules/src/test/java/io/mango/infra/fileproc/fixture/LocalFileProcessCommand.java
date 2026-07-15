package io.mango.infra.fileproc.fixture;

import io.mango.common.contract.LocalCapabilityContract;

import java.io.InputStream;

@LocalCapabilityContract
public record LocalFileProcessCommand(InputStream inputStream) {
}
