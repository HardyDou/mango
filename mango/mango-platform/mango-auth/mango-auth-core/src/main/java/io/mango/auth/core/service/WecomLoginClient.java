package io.mango.auth.core.service;

public interface WecomLoginClient {

    String getUserId(String corpId, String secret, String code);
}
