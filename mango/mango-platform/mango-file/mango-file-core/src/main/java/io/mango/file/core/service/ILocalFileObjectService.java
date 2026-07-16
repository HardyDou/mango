package io.mango.file.core.service;

import io.mango.file.core.storage.FileObject;

/** Reads objects from the configured local storage boundary. */
public interface ILocalFileObjectService {

    FileObject get(String bucket, String objectName);
}
