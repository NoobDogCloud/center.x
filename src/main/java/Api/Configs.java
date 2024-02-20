package main.java.Api;

import db.service.ApplicationTemplate;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

import java.util.HashSet;
import java.util.Set;

import static model.ConfigModel.middlewareTypeMap;

public class Configs extends ApplicationTemplate {
    public Configs() {
        super("configs");
    }

    public JSONObject support(String sdk_id) {
        SdkManager sdkManager = new SdkManager();
        JSONObject sdkInfo = sdkManager.find(sdk_id);
        if( JSONObject.isInvalided(sdkInfo) ) {
            return JSONObject.build();
        }
        JSONObject depTemplate = sdkInfo.getJson("dependencies_template");
        if( JSONObject.isInvalided(depTemplate) ) {
            return JSONObject.build();
        }
        JSONObject result = JSONObject.build();
        // 构造按分类的配置信息
        var configNames = depTemplate.keySet();
        for( String configName : configNames ) {
            String configType = middlewareTypeMap.get(configName);
            if( configType != null ) {
                result.putWithArray(configType, JSONObject.build("name", configName).put("id", configName));
            }
        }
        return result;
//        JSONObject result = JSONObject.build();
//        // 根据现有的配置，返回支持的配置信息
//        JSONArray<JSONObject> configArr = select();
//        JSONArray<JSONObject> dbArr = new JSONArray<>();
//        JSONArray<JSONObject> cacheArr = new JSONArray<>();
//        JSONArray<JSONObject> mqArr = new JSONArray<>();
//        JSONArray<JSONObject> storeArr = new JSONArray<>();
//        JSONArray<JSONObject> streamComputerArr = new JSONArray<>();
//        JSONArray<JSONObject> blockComputerArr = new JSONArray<>();
//        for (JSONObject o : configArr) {
//            String type = o.getString("type");
//            String name = o.getString("name");
//            switch (type) {
//                case "db" -> dbArr.add(JSONObject.build().put("name", name).put("id", name));
//                case "cache" -> cacheArr.add(JSONObject.build().put("name", name).put("id", name));
//                case "mq" -> mqArr.add(JSONObject.build().put("name", name).put("id", name));
//                case "store" -> storeArr.add(JSONObject.build().put("name", name).put("id", name));
//                case "streamComputer" -> streamComputerArr.add(JSONObject.build().put("name", name).put("id", name));
//                case "blockComputer" -> blockComputerArr.add(JSONObject.build().put("name", name).put("id", name));
//            }
//        }
//
//        return result.put("db", dbArr)
//                .put("cache", cacheArr)
//                .put("mq", mqArr)
//                .put("store", storeArr)
//                .put("streamComputer", streamComputerArr)
//                .put("blockComputer", blockComputerArr);
    }
}
