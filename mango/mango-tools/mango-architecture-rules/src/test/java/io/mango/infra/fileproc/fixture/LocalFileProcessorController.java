package io.mango.infra.fileproc.fixture;

import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;

@RestController
public final class LocalFileProcessorController implements LocalFileProcessorApi {

    @Override
    public byte[] process(InputStream inputStream) {
        return new byte[0];
    }
}
