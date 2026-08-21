package io.mango.infra.docsign;

import io.mango.common.contract.LocalCapabilityContract;
import io.mango.infra.docsign.command.DocumentSignCommand;
import io.mango.infra.docsign.command.DocumentVerifyCommand;
import io.mango.infra.docsign.enums.DocumentSignFormat;
import io.mango.infra.docsign.vo.DocumentSignResultVO;
import io.mango.infra.docsign.vo.DocumentSignStreamResultVO;
import io.mango.infra.docsign.vo.DocumentVerifyResultVO;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

/**
 * Local PDF/OFD document signing and validation capability.
 */
@LocalCapabilityContract
public interface DocumentSignApi {

    /**
     * Sign a document using the provider for its format.
     *
     * @param command signing command
     * @return signed document
     */
    DocumentSignResultVO sign(DocumentSignCommand command);

    /**
     * Sign a document with bounded heap usage. The caller owns both streams and this method does
     * not close them. Implementations may spool the input to protected temporary storage when the
     * document format requires random access.
     *
     * @param command signing options; embedded byte content is ignored
     * @param document source document stream
     * @param signedDocument target stream receiving the signed document
     * @return signed document metadata
     */
    DocumentSignStreamResultVO sign(DocumentSignCommand command,
                                    InputStream document,
                                    OutputStream signedDocument);

    /**
     * Validate every signature in a document.
     *
     * @param command validation command
     * @return validation summary
     */
    DocumentVerifyResultVO verify(DocumentVerifyCommand command);

    /**
     * Validate every signature with bounded heap usage. The caller owns the input stream and this
     * method does not close it. Implementations may spool it when random access is required.
     *
     * @param command validation options; embedded byte content is ignored
     * @param document signed document stream
     * @return validation summary
     */
    DocumentVerifyResultVO verify(DocumentVerifyCommand command, InputStream document);

    /**
     * Formats supported by the registered providers.
     *
     * @return immutable format set
     */
    Set<DocumentSignFormat> supportedFormats();
}
