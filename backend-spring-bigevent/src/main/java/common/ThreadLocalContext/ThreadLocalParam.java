package common.ThreadLocalContext;

import common.constant.JwtConstant;

import java.util.Map;

public class ThreadLocalParam {
    /**
     * 获取当前用户的用户名
     */
    public static String getUserName() {
        Map<String, Object> claims = ThreadLocalContextHolder.get();
        if (claims != null) {
            return claims.get(JwtConstant.NAME).toString();
        }
        return null;
    }

    /**
     * 获取当前用户的ID
     */
    public static Long getUserId() {
        Map<String, Object> claims = ThreadLocalContextHolder.get();
        if (claims != null) {
            String id = claims.get(JwtConstant.ID).toString();
            return Long.parseLong(id);
        }
        return null;
    }
}