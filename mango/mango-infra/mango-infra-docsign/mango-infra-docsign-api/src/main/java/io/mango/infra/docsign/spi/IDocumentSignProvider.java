package io.mango.infra.docsign.spi;

import io.mango.common.contract.LocalCapabilityContract;
import io.mango.infra.docsign.command.DocumentSignCommand;
import io.mango.infra.docsign.command.DocumentVerifyCommand;
import io.mango.infra.docsign.enums.DocumentSignFormat;
import io.mango.infra.docsign.vo.DocumentSignResultVO;
import io.mango.infra.docsign.vo.DocumentSignStreamResultVO;
import io.mango.infra.docsign.vo.DocumentVerifyResultVO;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Document-format-specific signing and validation provider.
 */
@LocalCapabilityContract
public interface IDocumentSignProvider {

    /**
     * Whether this provider supports a document format.
     *
     * @param format document format
     * @return true when supported
     */
    boolean supports(DocumentSignFormat format);

    /**
     * Sign a document.
     *
     * @param command signing command
     * @return signed document
     */
    DocumentSignResultVO sign(DocumentSignCommand command);

    /**
     * Sign a document from a caller-owned stream into a caller-owned stream.
     *
     * @param command signing options
     * @param document source document stream
     * @param signedDocument signed document target
     * @return signed document metadata
     */
    DocumentSignStreamResultVO sign(DocumentSignCommand command,
                                    InputStream document,
                                    OutputStream signedDocument);

    /**
     * Validate all signatures in a document.
     *
     * @param command validation command
     * @return validation summary
     */
    DocumentVerifyResultVO verify(DocumentVerifyCommand command);

    /**
     * Validate a document from a caller-owned stream.
     *
     * @param command validation options
     * @param document signed document stream
     * @return validation summary
     */
    DocumentVerifyResultVO verify(DocumentVerifyCommand command, InputStream document);
}
