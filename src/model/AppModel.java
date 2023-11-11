package model;

import common.java.Check.CheckHelper;
import main.java.Module.AppsBind;
import org.json.gsc.JSONObject;

public class AppModel {
    public static JSONObject removePrivateInfo(JSONObject info) {
        info.remove("k8s");
        info.remove("master");
        info.remove("secret");
        info.remove("state");
        info.remove("createat");
        info.remove("updateat");
        info.remove("desc");
        info.remove("category");
        return info;
    }

    public static boolean check_name(String name) {
        return CheckHelper.IsStrictID(name, 64) &&
                !name.equals("default") &&
                !name.equals("system") &&
                !name.startsWith("kube-");
    }

    // 确保应用绑定的用户服务实例有效
    public static boolean freshUserService(JSONObject info) {
        // 判断当前应用是否设置了运行集群
        var appBind = AppsBind.build();
        return appBind.keepUserService(info);
    }
}
