package model;

import common.java.Apps.MicroService.Config.ModelServiceConfig;
import common.java.Database.DBLayer;
import common.java.Number.NumberHelper;
import common.java.Rpc.FilterReturn;
import common.java.String.StringHelper;
import main.java.Api.Apps;
import main.java.Api.ConfigTemplates;
import main.java.Api.Configs;
import main.java.Api.ServicesDeploy;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class ConfigModel {
    public static HashMap<String, String> middlewareTypeMap = new HashMap<>() {{
        put("mongodb", "db");
        put("mysql", "db");
        put("h2", "db");
        put("oracle", "db");
        put("redis", "cache");
        put("pulsar", "mq");
        put("rabbitmq", "mq");
        put("kafka", "mq");
        put("activemq", "mq");
    }};

    // 根据配置分类定义获得有效配置信息
    public static JSONObject getConfigVal(JSONObject info) {
        JSONObject r = JSONObject.build();
        if (!JSONObject.isInvalided(info)) {
            Set<String> configKeyArr = ModelServiceConfig.getCfgKey();
            for (String className : configKeyArr) {
                if (info.containsKey(className)) {
                    r.put(className, info.get(className));
                }
            }
        }
        return r;
    }

    // 获得配置的名称
    public static Set<String> getConfigNameArr(JSONObject info) {
        Set<String> r = new HashSet<>();
        if (!JSONObject.isInvalided(info)) {
            Set<String> configKeyArr = ModelServiceConfig.getCfgKey();
            for (String className : configKeyArr) {
                if (info.containsKey(className)) {
                    r.add(info.getString(className));
                }
            }
        }
        return r;
    }

    // 检查配置类型是否合规
    public static boolean check_type(String typeName) {
        Set<String> cfgKey = new HashSet<>();
        // 新增，更新时 额外新增 other 配置字段
        cfgKey.add(ModelServiceConfig.ConfigKeyName.Other);
        cfgKey.addAll(ModelServiceConfig.getCfgKey());

        return cfgKey.contains(typeName);
    }

    // 检查配置名有效性
    public static boolean check_config(JSONObject config) {
        if (config == null) {
            return true;
        }
        JSONObject _config = JSONObject.build(config);
        if (_config.has(ModelServiceConfig.ConfigKeyName.Other)) {
            _config.remove(ModelServiceConfig.ConfigKeyName.Other);
        }
        Configs cfg = new Configs();
        DBLayer db = cfg.getDb().or();
        var it = _config.values().iterator();
        while (it.hasNext()) {
            var _val = it.next();
            if (_val instanceof String val) {
                if (val.length() > 0) {
                    db.eq("name", val);
                } else {
                    it.remove();
                }
            }
        }
        return (db.nullCondition() ? 0 : db.count()) == _config.size();
    }

    // 检查配置模板ID是否存在，配置是否与模板匹配
    private static int check_templateId(Object tid, JSONObject configInfo) {
        ConfigTemplates cfgTpl = new ConfigTemplates();
        DBLayer db = cfgTpl.getDb();
        JSONObject templateInfo = db.eq(db.getGeneratedKeys(), tid).find();
        if (JSONObject.isInvalided(templateInfo)) {
            return 1;       // 模板ID无效
        }
        JSONObject configStruct = templateInfo.getJson("template");
        if (JSONObject.isInvalided(configStruct)) {
            return 4;       // 模板ID无效
        }
        // 全部字段
        Set<String> fieldArr = new HashSet<>();
        // 必须存在
        Set<String> requiredArr = new HashSet<>();
        JSONArray<JSONObject> widgetList = configStruct.getJsonArray("widgetList");
        for (JSONObject v : widgetList) {
            JSONObject options = v.getJson("options");
            if (JSONObject.isInvalided(options)) {
                continue;
            }
            String name = options.getString("name");
            fieldArr.add(name);
            if (options.containsKey("required") && options.getBoolean("required")) {
                requiredArr.add(name);
            }
        }
        // 检查未定义字段
        for (String key : configInfo.keySet()) {
            if (!fieldArr.contains(key)) {
                return 2;       // 配置包含未定义字段
            }
        }
        // 检查必填字段
        for (String key : requiredArr) {
            if (!configInfo.containsKey(key)) {
                return 3;       // 未包含必填字段
            }
        }
        // 通过检查
        return 0;
    }


    public static boolean filterConfig(JSONObject info) {
        if (info.containsKey("config")) {
            JSONObject j = info.getJson("config");
            if (JSONObject.isInvalided(j)) {
                return false;
            }
            info.put("config", j);
        }
        return true;
    }

    public static FilterReturn checkTemplate(JSONObject info) {
        var _tid = info.get("templateid");
        if (!NumberHelper.isNumeric(_tid)) {
            return FilterReturn.build(false, "未配置配置模板");
        }
        // 模板id为0，不启用模板检测
        var tid = NumberHelper.number2int(_tid);
        if (NumberHelper.number2int(tid) == 0) {
            return FilterReturn.success();
        }
        JSONObject configInfo = info.getJson("config");
        if (JSONObject.isInvalided(configInfo)) {
            return FilterReturn.build(false, "配置信息[" + configInfo + "]不合法");
        }
        int v = check_templateId(tid, configInfo);
        return switch (v) {
            case 1 -> FilterReturn.build(false, "模板ID无效");
            case 2 -> FilterReturn.build(false, "包含未定义字段");
            case 3 -> FilterReturn.build(false, "必填字段未填");
            case 4 -> FilterReturn.build(false, "无效配置模板");
            default -> FilterReturn.success();
        };
    }

    public static FilterReturn deleteConfig(String[] idArr) {
        // 获得应用全部配置名
        Apps app = new Apps();
        String appCfgName = app.getConfigName();
        ServicesDeploy serviceDeploy = new ServicesDeploy();
        String serviceDeployCfgName = serviceDeploy.getConfigName();
        Set<String> appSet = StringHelper.build(appCfgName).toHashSet();
        Set<String> svcSet = StringHelper.build(serviceDeployCfgName).toHashSet();
        appSet.addAll(svcSet);
        // 获得要删除的配置名
        Configs cfg = new Configs();
        DBLayer db = cfg.getDb().or().field(new String[]{"name"});
        for (String id : idArr) {
            db.eq(db.getGeneratedKeys(), id);
        }
        JSONArray<JSONObject> result = JSONArray.build();
        if (!db.nullCondition()) {
            result = db.select();
        }
        for (JSONObject info : result) {
            String name = info.getString("name");
            if (appSet.contains(name)) {
                return FilterReturn.build(false, "当前配置正在使用，无法删除");
            }
        }
        return FilterReturn.success();
    }
}
