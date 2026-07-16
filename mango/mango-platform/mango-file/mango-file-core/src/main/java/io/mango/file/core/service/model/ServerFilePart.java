package io.mango.file.core.service.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

/** One file part received by the server-side multipart upload endpoint. */
@Getter
@RequiredArgsConstructor
public class ServerFilePart {

    private final Long sessionId;

    private final Integer partNumber;

    private final MultipartFile file;
}
