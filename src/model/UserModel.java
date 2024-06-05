package model;

import common.java.Encrypt.Md5;
import common.java.Http.Server.HttpContext;
import common.java.Jwt.Jwt;
import common.java.Jwt.JwtInfo;
import common.java.Time.TimeHelper;
import org.json.gsc.JSONObject;

public class UserModel {
    public static String EncodePassword(String userId, String password, String salt) {
        return Md5.build(password + "." + salt + "#" + userId);
    }

    public static JSONObject filterUserInfo(JSONObject userInfo) {
        JSONObject user = JSONObject.build(userInfo);
        user.remove("password");
        user.remove("salt");
        return user;
    }

    public static String buildJWT(String userName, JSONObject userInfo, long expireSeconds) {
        JSONObject safeUserInfo = UserModel.filterUserInfo(userInfo);
        return Jwt.createJwt(userName, safeUserInfo, expireSeconds);
    }

    // 取当前请求jwt
    public static boolean checkJwt(String userId) {
        // token转成jwt对象
        String token = HttpContext.current().sid();
        // 获得salt
        try {
            JwtInfo jwt = JwtInfo.buildBy(token);
            JSONObject userInfo = jwt.decodeJwt();
            // 解密后用户名相等
            return userInfo.getInt("level") >= 10000 || userInfo.getString("userid").equals(userId);
        } catch (Exception e) {
            return false;
        }
    }
}
