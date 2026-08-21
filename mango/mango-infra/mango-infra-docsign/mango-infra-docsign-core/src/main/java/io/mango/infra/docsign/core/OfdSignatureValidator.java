package io.mango.infra.docsign.core;

import org.ofdrw.core.signatures.SigType;
import org.ofdrw.gm.ses.parse.SESVersion;
import org.ofdrw.gm.ses.parse.SESVersionHolder;
import org.ofdrw.gm.ses.parse.VersionParser;
import org.ofdrw.sign.verify.SignedDataValidateContainer;
import org.ofdrw.sign.verify.container.GBT35275ValidateContainer;
import org.ofdrw.sign.verify.container.SESV1ValidateContainer;
import org.ofdrw.sign.verify.container.SESV4ValidateContainer;
import org.ofdrw.sign.verify.container.SESV5ValidateContainer;
import org.ofdrw.sign.verify.exceptions.InvalidSignedValueException;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Selects the OFDRW validator by digital-signature or electronic-seal encoding.
 */
final class OfdSignatureValidator implements SignedDataValidateContainer {

    @Override
    public void validate(SigType type, String algorithm, byte[] content, byte[] signedValue)
            throws InvalidSignedValueException, IOException, GeneralSecurityException {
        if (type == SigType.Sign) {
            new GBT35275ValidateContainer().validate(type, algorithm, content, signedValue);
            return;
        }
        SESVersionHolder version = VersionParser.parseSES_SignatureVersion(signedValue);
        if (version.getVersion() == SESVersion.v1) {
            new SESV1ValidateContainer().validate(type, algorithm, content, signedValue);
        } else if (version.getVersion() == SESVersion.v4) {
            new SESV4ValidateContainer().validate(type, algorithm, content, signedValue);
        } else if (version.getVersion() == SESVersion.v5) {
            new SESV5ValidateContainer().validate(type, algorithm, content, signedValue);
        } else {
            throw new InvalidSignedValueException("不支持的 OFD 电子印章版本");
        }
    }
}
