package main.java.Api;

import common.java.Database.DBLayer;
import common.java.Rpc.RpcMessage;
import common.java.String.StringHelper;
import db.service.ApplicationTemplate;
import model.UserModel;
import org.json.gsc.JSONObject;

public class Users extends ApplicationTemplate {
    public Users() {
        super("users");
    }

    // 注册
    public Object register(JSONObject info) {
        return getDb().data(info).insertOnce();
    }

    // 注销
    public Object logoff(String userId) {
        return getDb().eq("userid", userId).delete();
    }

    // 编辑用户信息
    public Object edit(String userId, JSONObject info) {
        DBLayer db = getDb();
        boolean r = db.eq("userid", userId).data(info).update();
        if (!r) {
            return false;
        }
        JSONObject userInfo = db.eq("userid", userId).find();
        if (JSONObject.isInvalided(userInfo)) {
            return RpcMessage.Instant(false, "当前用户不存在!");
        }
        return UserModel.buildJWT(userInfo, userInfo.getString("salt"));
    }

    // 修改密码(需要重新登录)
    public Object change_password(String userId, String password, String new_password) {
        DBLayer db = getDb();
        JSONObject userInfo = getDb().eq("userid", userId).find();
        if (JSONObject.isInvalided(userInfo)) {
            return RpcMessage.Instant(false, "当前用户不存在!");
        }
        String encode_password = UserModel.EncodePassword(userId, password, userInfo.getString("salt"));
        // 验证当前密码
        if (!userInfo.getString("password").equals(encode_password)) {
            return RpcMessage.Instant(false, "当前密码错误!");
        }
        // 重设密码和salt
        String new_salt = StringHelper.randomString(5);
        String new_encode_password = UserModel.EncodePassword(userId, new_password, new_salt);
        return db.eq("userid", userId).data(JSONObject.build()
                .put("password", new_encode_password)
                .put("salt", new_salt)
        ).update();
    }

    // 登录
    public Object login(String userId, String password) {
        JSONObject userInfo = getDb().eq("userid", userId).find();
        if (JSONObject.isInvalided(userInfo)) {
            return RpcMessage.Instant(false, "用户 " + userId + " 不存在!");
        }
        if (!userInfo.getString("password").equals(UserModel.EncodePassword(userId, password, userInfo.getString("salt")))) {
            return RpcMessage.Instant(false, "密码错误!");
        }
        JSONObject safeUserInfo = UserModel.filterUserInfo(userInfo);
        return safeUserInfo.put("token", UserModel.buildJWT(safeUserInfo, userInfo.getString("salt")));
    }

    // 登出
    public Object logout(String userId) {
        return true;
    }

    // 存在检查
    public boolean isExisting(String userid) {
        JSONObject userInfo = getDb().eq("userid", userid).find();
        return !JSONObject.isInvalided(userInfo);
    }

    // 生成密码（测试接口）
    public Object showPassword(String userId, String new_password) {
        // 重设密码和salt
        String new_salt = StringHelper.randomString(5);
        String new_encode_password = UserModel.EncodePassword(userId, new_password, new_salt);
        System.out.println();
        System.out.println("salt    :" + new_salt);
        System.out.println("password:" + new_encode_password);
        return JSONObject.build().put("salt", new_salt).put("password", new_encode_password);
    }
}
