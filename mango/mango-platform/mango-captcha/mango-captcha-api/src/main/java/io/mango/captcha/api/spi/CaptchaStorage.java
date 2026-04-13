package io.mango.captcha.api.spi;

/**
 * 验证码存储接口
 *
 * @author Mango
 */
public interface CaptchaStorage {

    /**
     * 保存验证码
     *
     * @param key   验证码key
     * @param value 验证码值
     * @param ttl   过期时间（秒）
     */
    void save(String key, String value, long ttl);

    /**
     * 获取验证码
     *
     * @param key 验证码key
     * @return 验证码值
     */
    String get(String key);

    /**
     * 删除验证码
     *
     * @param key 验证码key
     */
    void remove(String key);

    /**
     * 检查是否存在
     *
     * @param key 验证码key
     * @return true-存在
     */
    boolean exists(String key);

    /**
     * 获取存储类型
     *
     * @return 存储类型
     */
    String getType();
}
