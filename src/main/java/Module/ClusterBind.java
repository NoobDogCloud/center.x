package main.java.Module;

import db.service.BaseTemplate;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

public class ClusterBind {
    private ClusterBind() {
    }

    public static ClusterBind build() {
        return new ClusterBind();
    }

    public static JSONObject getInfo(int id) {
        var db = (new BaseTemplate("k8s")).getDb();// (new Cluster()).getDb();
        return db.eq(db.getGeneratedKeys(), id).find();
    }

    public boolean update(int id, JSONObject newInfo) {
        JSONObject info = getInfo(id);
        if (JSONObject.isInvalided(info)) {
            return false;
        }
        var keys = info.getNeField(newInfo);
        if (keys.isEmpty()) {
            return false;
        }

        if (keys.contains("config") || keys.contains("registry")) {
            var appArr = AppsBind.getInfoByClusterId(id);
            if (!appArr.isEmpty()) {
                var appsBind = AppsBind.build();
                for (var app : appArr) {
                    // 集群授权配置信息更新
                    if (keys.contains("config")) {
                        appsBind.update(app.getString("id"), JSONObject.build(), JSONArray.<String>build().put("k8s"));
                    }
                    // 集群私库信息更新
                    if (keys.contains("registry")) {
                        appsBind.updateRegistry(info, app.getString("name"));
                    }
                }
            }
        }
        return true;
    }
}
