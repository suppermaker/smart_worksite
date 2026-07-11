package com.xd.smartworksite.common.redis;

public final class RedisKeys {

    private static final String PREFIX = "smart-worksite";

    private RedisKeys() {
    }

    public static String cache(String name) {
        return PREFIX + ":cache:" + name;
    }

    public static String lock(String name) {
        return PREFIX + ":lock:" + name;
    }

    public static String queue(String name) {
        return PREFIX + ":queue:" + name;
    }

    public static String tokenBlacklist(String jti) {
        return PREFIX + ":auth:token-blacklist:" + jti;
    }

    public static String loginFailureCount(String username) {
        return PREFIX + ":auth:login-failure:" + username;
    }

    public static String loginLock(String username) {
        return PREFIX + ":auth:login-lock:" + username;
    }
}
