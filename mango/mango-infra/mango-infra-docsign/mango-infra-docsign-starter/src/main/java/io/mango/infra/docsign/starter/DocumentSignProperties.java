package io.mango.infra.docsign.starter;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.nio.file.Path;

/**
 * Document signing auto-configuration properties.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "mango.docsign")
public class DocumentSignProperties {

    private static final long DEFAULT_MAX_IN_MEMORY_MEGABYTES = 16L;

    /** Whether document signing is enabled. */
    private boolean enabled = true;

    /** Whether the PDF provider is registered. */
    private boolean pdfEnabled = true;

    /** Whether the OFD provider is registered. */
    private boolean ofdEnabled = true;

    /** Maximum document size accepted by the compatibility byte-array API. */
    private DataSize maxInMemorySize = DataSize.ofMegabytes(DEFAULT_MAX_IN_MEMORY_MEGABYTES);

    /** Maximum source document size accepted by streaming processing. */
    private DataSize maxDocumentSize = DataSize.ofGigabytes(2);

    /** Directory used for short-lived random-access document files. */
    private Path temporaryDirectory = Path.of(System.getProperty("java.io.tmpdir"), "mango-docsign");
}
