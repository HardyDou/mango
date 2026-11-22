package io.mango.authorization.api.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明 HTTP 端点参与无状态数据权限 Grant 签发或校验。
 * <p>
 * 列表端点使用本注解后，响应头会带上本页可见数据 ID 集合的 Grant；详情端点使用本注解后，
 * 请求必须携带同一 {@link #domain()} 的 Grant，且请求中的记录 ID 必须落在 Grant 集合内。
 * <p>
 * 与 {@link ApiAccess} 语义正交：{@code ApiAccess} 回答「谁能调用该接口」，本注解回答
 * 「该记录 ID 是否在调用方刚看到的列表里」。路径或参数上不做任何隐式约定。
 */
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DataGrant {

    /**
     * 数据权限域标识，必须是 {@code DataScopeProvider} 认可的 resourceCode（例如 {@code workflow:business-apply}），
     * 列表签发与详情校验必须使用同一个值；同时用于数据范围指纹（{@code pv}）解析。
     *
     * @return 数据权限域标识。
     */
    String domain();

    /**
     * 记录 ID 的类型，用于将响应体中的 ID 字段归一化为 Grant 集合元素。
     *
     * @return 记录 ID 类型，当前支持数值型与可解析为数值的字符串型。
     */
    Class<?> idType() default Long.class;

    /**
     * 列表响应体中承载记录 ID 的字段名。
     *
     * @return 记录 ID 字段名。
     */
    String idField() default "id";
}
