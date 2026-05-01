package io.mango.authorization.api.security;

import java.util.Map;

/**
 * Token 技术能力接口。
 * <p>
 * 提供 JWT token 生成、校验和刷新能力。
 * 默认实现：{@code JjwtTokenServiceImpl}，底层使用 JJWT。
 *
 * @author Mango
 */
public interface ITokenProvider {

    /** Authorization 请求头中的 Bearer token 前缀。 */
    String BEARER_PREFIX = "Bearer ";

    /** Token 类型：access token。 */
    String TOKEN_TYPE_ACCESS = "access";

    /** Token 类型：refresh token。 */
    String TOKEN_TYPE_REFRESH = "refresh";

    /**
     * 生成 access token，默认短期有效。
     *
     * @param userId      用户 ID
     * @param username    用户名
     * @param extraClaims 需要写入 token 的扩展 claim
     * @return access token 字符串
     */
    String generateAccessToken(Long userId, String username, Map<String, Object> extraClaims);

    /**
     * 生成 refresh token，默认长期有效。
     *
     * @param userId   用户 ID
     * @param username 用户名
     * @return refresh token 字符串
     */
    String generateRefreshToken(Long userId, String username);

    /**
     * 生成 refresh token，并写入扩展 claim。
     *
     * @param userId      用户 ID
     * @param username    用户名
     * @param extraClaims 需要写入 token 的扩展 claim
     * @return refresh token 字符串
     */
    default String generateRefreshToken(Long userId, String username, Map<String, Object> extraClaims) {
        return generateRefreshToken(userId, username);
    }

    /**
     * 校验 token 签名有效且未过期。
     *
     * @param token JWT token 字符串
     * @return 有效返回 true，否则返回 false
     */
    boolean validateToken(String token);

    /**
     * 从 token 中读取用户 ID。
     *
     * @param token JWT token 字符串
     * @return 用户 ID；token 无效时返回 null
     */
    Long getUserId(String token);

    /**
     * 从 token 中读取用户名。
     *
     * @param token JWT token 字符串
     * @return 用户名；token 无效时返回 null
     */
    String getUsername(String token);

    /**
     * 从 token 中读取 token 类型。
     *
     * @param token JWT token 字符串
     * @return access、refresh；token 无效时返回 null
     */
    String getTokenType(String token);

    /**
     * 从 token 中读取指定 claim。
     *
     * @param token JWT token 字符串
     * @param claimName claim 名称
     * @return claim 字符串值；不存在或 token 无效时返回 null
     */
    default String getClaim(String token, String claimName) {
        return null;
    }

    /**
     * 使用有效的 refresh token 刷新 token 对。
     *
     * @param refreshToken 有效的 refresh token
     * @return 新 token 对；refresh token 无效时返回 null
     */
    TokenPair refresh(String refreshToken);

    /**
     * 刷新响应使用的 token 对。
     */
    record TokenPair(String accessToken, String refreshToken) {}
}
