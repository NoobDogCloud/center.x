package model;

import common.java.Database.DBLayer;
import common.java.Rpc.FilterReturn;
import common.java.String.StringHelper;
import main.java.Api.Apps;
import main.java.Api.Services;
import main.java.Api.ServicesDeploy;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

public class ServiceDeployModel {
    public static JSONObject removePrivateInfo(JSONObject info) {
        info.remove("replicaset");
        info.remove("parameter");
        info.remove("debug");
        info.remove("createat");
        info.remove("updateat");
        info.remove("state");
        // info.remove("clusteraddr");
        return info;
    }

    public static FilterReturn deleteDeploy(String[] idArr) {
        // 删除应用时，连带删除对应部署
        ServicesDeploy ssd = new ServicesDeploy();
        DBLayer db = ssd.getDb().or();
        for (String id : idArr) {
            if (!StringHelper.isInvalided(id)) {
                db.eq("appid", id);
            }
        }
        long l = (!db.nullCondition()) ? db.deleteAll() : 0;
        return FilterReturn.success();
    }

    // 检查应用id
    public static boolean check_appId(String id) {
        Apps app = new Apps();
        DBLayer db = app.getDb();
        JSONObject info = db.eq(db.getGeneratedKeys(), id).find();
        return !JSONObject.isInvalided(info);
    }

    // 检查服务id
    public static boolean check_serviceId(Object id) {
        Services service = new Services();
        DBLayer db = service.getDb();
        JSONObject info = db.eq(db.getGeneratedKeys(), id).find();
        return !JSONObject.isInvalided(info);
    }

    // 检查代理服务
    public static JSONArray<String> check_proxy(String appId, JSONArray<String> proxyService) {
        ServicesDeploy serviceDeploy = new ServicesDeploy();
        DBLayer db = serviceDeploy.getDb();
        JSONArray<String> result = JSONArray.build();
        for (var proxy_name : proxyService) {
            JSONObject deployInfo = db.eq("name", proxy_name).eq("appid", appId).find();
            if (!JSONObject.isInvalided(deployInfo)) {
                result.add(proxy_name);
            }
        }
        return result;
    }
}
