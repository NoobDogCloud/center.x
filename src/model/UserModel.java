package model;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import common.java.Encrypt.Md5;
import common.java.Http.Server.HttpContext;
import common.java.Time.TimeHelper;
import main.java.Api.Users;
import org.json.gsc.JSONObject;

import java.util.Date;
import java.util.HashMap;

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

    public static String buildJWT(JSONObject userInfo, String salt) {
        JSONObject safeUserInfo = UserModel.filterUserInfo(userInfo);
        long ft = TimeHelper.build().nowMillis() + (86400 * 1000);
        userInfo.put("failure_time", ft);
        return JWT.create()
                .withHeader(new HashMap<>())
                .withClaim("user", safeUserInfo)
                .withExpiresAt(new Date(ft))
                .sign(Algorithm.HMAC256(salt));
    }

    // 取当前请求jwt
    public static boolean checkJwt(String userId) {
        // token转成jwt对象
        String token = HttpContext.current().sid();
        // 获得salt
        try {
            Users users = new Users();
            JSONObject userInfo = users.getDb().eq("userid", userId).field(new String[]{"salt"}).find();
            String salt = userInfo.getString("salt");
            JWTVerifier jwtVerifier = JWT.require(Algorithm.HMAC256(salt)).build();
            DecodedJWT decodedJWT = jwtVerifier.verify(token);
            JSONObject jwt = JSONObject.build(decodedJWT.getClaim("user").asMap());
            // 解密后用户名相等
            return jwt.getInt("level") >= 10000 || jwt.getString("userid").equals(userId);
        } catch (Exception e) {
            return false;
        }
    }
}
