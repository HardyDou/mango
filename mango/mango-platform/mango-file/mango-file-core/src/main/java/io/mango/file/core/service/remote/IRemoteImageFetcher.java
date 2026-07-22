package io.mango.file.core.service.remote;

/** Fetches and validates a public remote image without forwarding caller credentials. */
@FunctionalInterface
public interface IRemoteImageFetcher {

    /**
     * Fetches one remote image under the configured security limits.
     *
     * @param sourceUrl untrusted source URL
     * @return validated image content
     */
    RemoteImageContent fetch(String sourceUrl);
}
