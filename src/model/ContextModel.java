package model;

import common.java.Apps.MicroService.Model.MModelSuperField;
import common.java.Database.DBLayer;
import common.java.String.StringHelper;
import main.java.Api.Services;
import main.java.Api.ServicesDeploy;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

public class ContextModel {

    // 获得 合并了 service 和 deploy 信息的 服务信息
    public static JSONObject getFullDeployInfo(Object deployId) {
        // 获得简单部署信息
        ServicesDeploy sd = new ServicesDeploy();
        JSONObject deployInfo = sd.find(deployId);
        if (JSONObject.isInvalided(deployInfo)) {   // 部署信息无效
            return deployInfo;
        }
        // 获得部署对应服务信息
        Services s = new Services();
        JSONObject serviceInfo = s.find(deployInfo.get("serviceId"));
        if (JSONObject.isInvalided(deployInfo)) {   // 服务信息无效
            return serviceInfo;
        }
        return serviceInfo.put(deployInfo);
    }

    public static void fixDeployNetInfo(JSONObject deployInfo, int port) {
        String clusterInfo = deployInfo.getString("clusteraddr");
        if (StringHelper.isInvalided(clusterInfo)) {
            deployInfo.put("clusteraddr", "127.0.0.1:" + port);
        }
        String subInfo = deployInfo.getString("subaddr");
        if (StringHelper.isInvalided(subInfo)) {
            deployInfo.put("subaddr", "127.0.0.1:" + port);
        }
    }

    // 获得appId应用的 合并了 service 和 deploy 信息的 服务信息
    public static JSONObject getAppFullDeployInfo(String appId) {
        JSONObject r = new JSONObject();
        // 获得简单部署信息
        ServicesDeploy sd = new ServicesDeploy();
        DBLayer deployDb = sd.getDb();
        Services s = new Services();
        deployDb.eq("appId", appId).scan(arr -> {
            // 获得部署对应服务信息
            for (JSONObject deployInfo : arr) {
                JSONObject serviceInfo = s.find(deployInfo.get("serviceid"));
                if (!JSONObject.isInvalided(deployInfo)) {   // 服务信息无效
                    fixDeployNetInfo(deployInfo, serviceInfo.getInt("port"));
                    r.put(deployInfo.getString("name"), serviceInfo.put(deployInfo));
                }
            }
            return null;
        }, 50);
        return r;
    }

    // 给每一个模型补充系统默认模型描述
    public static JSONArray<JSONObject> appendDefaultModelInfo(JSONArray<JSONObject> model) {
        // 包含同名字段不增加
        var systemDefaultNode = MModelSuperField.defaultNodeJson();
        var keys = model.mapsByKey("name");
        for (JSONObject info : systemDefaultNode) {
            String key = info.getString("name");
            if (!keys.has(key)) {
                model.add(info);
            }
        }
        return model;
    }
}
