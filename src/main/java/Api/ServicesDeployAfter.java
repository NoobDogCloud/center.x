package main.java.Api;

import common.java.Rpc.RpcAfter;
import common.java.Rpc.RpcPageInfo;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

import java.util.Map;

public class ServicesDeployAfter extends RpcAfter {
    public ServicesDeployAfter() {
        filter(new String[]{"page", "pageEx"}, (funcName, parameter, returnValue) -> {
            RpcPageInfo r = (RpcPageInfo) returnValue;
            AppendInfoArray(r.info());
            return returnValue;
        }).filter(new String[]{"select", "selectEx"}, (funcName, parameter, returnValue) -> {
            AppendInfoArray((JSONArray<JSONObject>) returnValue);
            return returnValue;
        }).filter("find", (funcName, parameter, returnValue) -> {
            JSONObject appCache = new JSONObject();
            JSONObject svcCache = new JSONObject();
            Apps a = new Apps();
            Services s = new Services();
            JSONObject result = JSONObject.build();
            result.put((Map<? extends String, ?>) returnValue);
            AppendInfo(result, appCache, svcCache, a, s);
            return result;
        });
    }

    public static void AppendInfoArray(JSONArray<JSONObject> returnValue) {
        JSONObject appCache = new JSONObject();
        JSONObject svcCache = new JSONObject();
        JSONArray<JSONObject> result = JSONArray.build();
        result.put(returnValue);
        Apps a = new Apps();
        Services s = new Services();
        for (JSONObject item : result) {
            AppendInfo(item, appCache, svcCache, a, s);
        }
    }

    public static void AppendInfo(JSONObject item, JSONObject appCache, JSONObject svcCache, Apps a, Services s) {
        String appId = item.getString("appid");
        if (!appCache.has(appId)) {
            JSONObject appInfo = a.getDb().eq("id", appId).find();
            if (!JSONObject.isInvalided(appInfo)) {
                appCache.put(appId, appInfo);
            }
        }
        // 补充应用名
        JSONObject aInfo = appCache.getJson(appId);
        if (!JSONObject.isInvalided(aInfo)) {
            item.put("appname", aInfo.getString("name"));
        }

        String serviceId = String.valueOf(item.getInt("serviceid"));
        if (!svcCache.has(serviceId)) {
            JSONObject serviceInfo = s.getDb().eq("id", serviceId).find();
            if (!JSONObject.isInvalided(serviceInfo)) {
                svcCache.put(serviceId, serviceInfo);
            }
        }
        // 补充服务名和分类
        JSONObject sInfo = svcCache.getJson(serviceId);
        if (!JSONObject.isInvalided(sInfo)) {
            item.put("servicename", sInfo.getString("name"));
            item.put("service_category", sInfo.getString("category"));
            item.put("kind", sInfo.getString("kind"));
        }
        // 补充外部访问信息
        /*
        if( item.getString("subaddr").isEmpty() ){
            ServicesDeployBind.build().updateEndpoint(item, aInfo);
        }
        */
    }
}
