package io.mango.auth.core.service;

/**
 * Manages external identity avatar snapshots in Mango File.
 */
public interface IExternalIdentityAvatarService {

    /**
     * Imports an external avatar into the current tenant's managed file storage.
     *
     * @param userId Mango user that owns the external identity
     * @param sourceUrl public avatar source URL returned by the identity provider
     * @return managed file ID
     */
    Long importAvatar(Long userId, String sourceUrl);

    /**
     * Deletes an avatar snapshot that is no longer referenced.
     *
     * @param fileId managed file ID
     */
    void deleteAvatar(Long fileId);
}
