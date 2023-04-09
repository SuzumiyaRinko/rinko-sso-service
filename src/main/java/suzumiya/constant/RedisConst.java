package suzumiya.constant;

public class RedisConst {

    /* User */
    // 某个用户的访问API的频率
    public static final String USER_FREQUENCY_KEY = "user:frequency:"; // user:frequency:
    // 某个用户是否被ban掉（30s）
    public static final String USER_BAN_KEY = "user:ban:"; // user:ban:{ip}
    // 密码错误的重试次数
    public static final String LOGIN_RETRY_USER_KEY = "login:retry:user:"; // login:retry:user:{ip}
    // 验证码
    public static final String USER_VERIFY_KEY = "user:verify:"; // user:verify:{uuid}
    // 已经登录了的用户的用户信息
    public static final String LOGIN_USER_KEY = "login:user:"; // login:retry:user:{userId}
    // 需要在30mins内激活账号的userId
    public static final String ACTIVATION_USER_KEY = "activate:user:"; // activate:user:{activationUUID}
    // 某IP 24小时内的注册次数
    public static final String REGISTER_TIMES_KEY = "registerTimes:"; // registerTimes:{ip}

    /* Apps */
    // app列表
    public static final String APPS_KEY = "apps";
}
