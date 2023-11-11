package main.java.Api;

import common.java.Rpc.FilterReturn;
import common.java.Rpc.RpcBefore;
import main.java.Bean.UserBean;
import model.UserModel;
import org.json.gsc.JSONObject;

public class UsersBefore extends RpcBefore {
    // UserBean userInfo = ((JSONObject) paramArr[0]).mapper(UserBean.class);
    public UsersBefore() {
        filter("register", (func, paramArr) -> {
            // 检测必要字段是否都在同时取必要字段
            JSONObject result = JSONObject.build();
            JSONObject user = (JSONObject) paramArr[0];
            for (String fieldName : UserBean.getFields()) {
                Object v = user.get(fieldName);
                // 缺失字段
                if (v == null) {
                    return FilterReturn.build(false, "[" + fieldName + "]需要填写");
                }
                result.put(fieldName, v);
            }
            // 检查 userid 重复度
            Users users = new Users();
            if (!users.isExisting(user.getString("userid"))) {
                return FilterReturn.build(false, "当前用户名已存在");
            }
            // name,title,phone,password不得为空
            String[] check_fields = new String[]{"name", "title", "phone", "password"};
            for (String fieldName : check_fields) {
                if (user.getString(fieldName).length() < 3) {
                    return FilterReturn.build(false, "[" + fieldName + "] 为空!");
                }
            }
            paramArr[0] = result;
            return FilterReturn.success();
        }).filter("update", (func, paramArr) -> {
            String userId = (String) paramArr[0];
            if (!UserModel.checkJwt(userId)) {
                return FilterReturn.build(false, "会话异常!");
            }
            // 只允许修改 avatar，name，title，phone
            String[] allow_fields = new String[]{"avatar", "name", "title", "phone"};
            JSONObject info = (JSONObject) paramArr[1];
            JSONObject result_info = JSONObject.build();
            for (String key : allow_fields) {
                result_info.put(key, info.get(key));
            }
            paramArr[1] = result_info;
            return FilterReturn.success();
        }).filter("logout", (func, paramArr) -> {
            String userId = (String) paramArr[0];
            if (!UserModel.checkJwt(userId)) {
                return FilterReturn.build(false, "会话异常!");
            }
            return FilterReturn.success();
        });
    }
}
