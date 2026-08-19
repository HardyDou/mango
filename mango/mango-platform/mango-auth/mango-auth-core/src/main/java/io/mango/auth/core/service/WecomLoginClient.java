package io.mango.auth.core.service;

public interface WecomLoginClient {

    String getUserId(String corpId, String secret, String code);

    WecomUserProfile getUserProfile(String corpId, String secret, String userId);

    record WecomUserProfile(String userId, String displayName, String avatarUrl) {
    }
}
