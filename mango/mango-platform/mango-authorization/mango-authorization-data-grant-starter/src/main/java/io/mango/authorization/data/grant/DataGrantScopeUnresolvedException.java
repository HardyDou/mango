package io.mango.authorization.data.grant;

/**
 * 数据范围指纹解析失败。
 * <p>
 * 与「当前部署没有数据范围实现」区分：那种情况指纹为空、照常签发；本异常表示数据范围实现存在但取不到规则，
 * 属于无法证明数据范围的状态，必须按未签发处理（fail-closed），不能静默留空。
 */
class DataGrantScopeUnresolvedException extends IllegalStateException {

    private static final long serialVersionUID = 1L;

    /**
     * 构造异常。
     *
     * @param domain    数据权限域标识。
     * @param cause     原始异常。
     */
    DataGrantScopeUnresolvedException(String domain, Throwable cause) {
        super("数据范围指纹解析失败: domain=" + domain, cause);
    }
}
