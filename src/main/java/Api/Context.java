package main.java.Api;

import common.java.Apps.MicroService.MicroServiceContext;
import common.java.Apps.Roles.AppRolesDef;
import common.java.Config.Config;
import common.java.Database.DBLayer;
import common.java.Rpc.RpcMessage;
import common.java.String.StringHelper;
import model.*;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

import java.util.HashSet;
import java.util.Set;

public class Context {
    /**
     * @apiNote 获得指定应用上下文，融合了 service 和 serviceDeploy 数据
     */
    public Object get(String appId) {
        Apps app = new Apps();
        DBLayer appDb = app.getDb();
        // 应用id 无效
        JSONObject appInfo = appDb.eq(appDb.getGeneratedKeys(), appId).find();
        if (JSONObject.isInvalided(appInfo)) {
            return RpcMessage.Instant(false, "无效应用ID!");
        }
        // 删除应用隐私数据
        AppModel.removePrivateInfo(appInfo);
        // 完善了 roles 结构
        JSONObject currentRoles = appInfo.getJson("roles");
        if (JSONObject.isInvalided(currentRoles)) {
            currentRoles = JSONObject.build();
        }
        currentRoles.put(AppRolesDef.defaultRoles());
        appInfo.put("roles", currentRoles);

        // 补充了 services 结构
        JSONObject serviceArr = ContextModel.getAppFullDeployInfo(appId);
        for (Object v : serviceArr.values()) {
            JSONObject item = (JSONObject) v;
            ServiceDeployModel.removePrivateInfo(ServiceModel.removePrivateInfo(item));
        }
        return appInfo.put("services", serviceArr);
    }

    public JSONObject defaultRoles() {
        return JSONObject.build(AppRolesDef.defaultRoles());
    }

    /**
     * @param serviceDeployName 微服务部署名称(内部)
     * @param _appId            订阅服务所属应用ID
     * @apiNote 根据微服务名称获得其需要的 应用详细，配置详细，部署（实例）详细 服务有关的 应用,配置,服务（融合了部署的）
     */
    public JSONObject sub(String serviceDeployName, String _appId) {
        DBLayer deployDb =  ServicesDeploy.getPureDb();
        Set<String> cfgNmArr = new HashSet<>();
        JSONArray<JSONObject> deployArrInfo = JSONArray.build();
        DBLayer serviceDb = Services.getPureDb();
        DBLayer appDb = Apps.getPureDb();
        var appArrInfo = JSONArray.build();
        if (!StringHelper.isInvalided(_appId)) {
            deployDb.eq("appId", _appId);
        }
        deployDb.scan(
                arr -> {
                    for (var deployInfo : arr) {
                        if (!JSONObject.isInvalided(deployInfo)) {   // 部署信息有效
                            String appId = deployInfo.getString("appid");
                            String serviceId = deployInfo.getString("serviceid");
                            // 使用内部连接不为空时,替换外部链接方式
                            fixSubEndPeer(deployInfo);
                            JSONObject _serviceInfo = serviceDb.eq(serviceDb.getGeneratedKeys(), serviceId).find();
                            if (JSONObject.isInvalided(_serviceInfo)) {
                                _serviceInfo = JSONObject.build();
                            }
                            deployInfo = ServiceModel.removePrivateInfo(_serviceInfo).put(ServiceDeployModel.removePrivateInfo(deployInfo));
                            deployArrInfo.add(deployInfo);
                            // 记录配置名
                            if (deployInfo.getString("name").equals(serviceDeployName)) {
                                cfgNmArr.addAll(ConfigModel.getConfigNameArr(deployInfo.getJson("config")));
                            }
                            // 获得该服务对应的应用
                            JSONObject appInfo = appDb.eq(appDb.getGeneratedKeys(), appId).find();
                            // 补充应用角色信息
                            JSONObject currentRoles = appInfo.getJson("roles");
                            if (JSONObject.isInvalided(currentRoles)) {
                                currentRoles = JSONObject.build();
                            }
                            currentRoles.put(AppRolesDef.defaultRoles());
                            appInfo.put("roles", currentRoles);
                            // 删除应用秘钥，主控，k8s
                            AppModel.removePrivateInfo(appInfo);
                            cfgNmArr.addAll(ConfigModel.getConfigNameArr(appInfo.getJson("config")));
                            String mq_config = appInfo.getString("mq_config");
                            if (!StringHelper.isInvalided(mq_config)) {
                                cfgNmArr.add(mq_config);
                            }
                            appArrInfo.put(appInfo);
                        }
                    }
                    return null;
                }, 50
        );

        // 获得配置信息
        JSONArray<JSONObject> cfgArrInfo = new JSONArray<>();
        if (cfgNmArr.size() > 0) {
            DBLayer configDb = Configs.getPureDb().or();
            for (String cfgName : cfgNmArr) {
                configDb.eq("name", cfgName);
            }
            cfgArrInfo = configDb.select();
        }

        // 人工补充system部署信息
        deployArrInfo.add(buildSystemDeploy());
        // 构造返回信息
        return JSONObject.build().put("apps", appArrInfo)
                .put("services", deployArrInfo)
                .put("configs", cfgArrInfo);
    }

    public JSONObject sub(String serviceDeployName) {
        return sub(serviceDeployName, null);
    }

    // 构造开发用的应用部署信息
    public Object devContext(String appId, String devSecret) {
        Apps app = new Apps();
        DBLayer appDb = app.getDb();
        // 应用id 无效
        JSONObject appInfo = appDb.eq(appDb.getGeneratedKeys(), appId).eq("dev_secret", devSecret).find();
        if (JSONObject.isInvalided(appInfo)) {
            return RpcMessage.Instant(false, "无效应用ID!");
        }
        JSONArray<JSONObject> serviceResult = JSONArray.build();
        // 补充了 services 结构
        JSONObject serviceArr = ContextModel.getAppFullDeployInfo(appId);
        for (String name : serviceArr.keySet()) {
            JSONObject item = (JSONObject) serviceArr.get(name);
            JSONObject dataModel = item.getJson("datamodel");
            if (JSONObject.isInvalided(dataModel)) {
                continue;
            }
            JSONArray<JSONObject> rules = JSONArray.build();
            for (String modelName : dataModel.keySet()) {
                JSONObject v = dataModel.getJson(modelName);
                JSONObject info = JSONObject.build();
                info.put("name", modelName)
                        .put("text", v.getString("text"))
                        .put("primaryKey", v.getString("primaryKey"))
                        .put("textKey", v.getString("textKey"))
                        // 每一个模型规则组增加默认的框架字段规则
                        .put("rules", ContextModel.appendDefaultModelInfo(v.getJsonArray("rule")));
                rules.add(info);
            }
            ServiceDeployModel.removePrivateInfo(ServiceModel.removePrivateInfo(item));

            JSONObject serviceInfo = JSONObject.build().put("name", name).put("model", rules);
            serviceResult.add(serviceInfo);
        }
        return serviceResult;
    }

    private void fixSubEndPeer(JSONObject deployInfo) {
        deployInfo.putIfAbsent("clusteraddr", "");
        deployInfo.putIfAbsent("subaddr", "");
        // 内网发布模式时,内网连接信息替换连接点
        var openType = deployInfo.getInt("open");
        if (openType == 0) {
            String clusterAddr = deployInfo.getString("clusteraddr");
            if (!StringHelper.isInvalided(clusterAddr)) {
                deployInfo.put("subaddr", deployInfo.getString("clusteraddr"));
            }
        }
    }

    private JSONObject buildSystemDeploy() {
        return JSONObject.build()
                .put("id", 0)
                .put("appid", "0")
                .put("version", "3.0.0")
                .put("peeraddr", "")
                .put("serviceid", 0)
                .put("config", "")
                .put("secure", 0)
                .put("debug", 0)
                .put("open", 1)
                .put("transfer", MicroServiceContext.TransferKeyName.Http)
                .put("port", Config.port)
                .put("category", "classic")
                .put("name", "system")
                .put("datamodel", "")
                .put("subaddr", Config.getMasterUrl())
                .put("clusteraddr", Config.getMasterUrl())
                .put("proxy_target", JSONArray.<String>build().toString())
                .put("target_port", Config.port)
                .put("kind", "system");
    }
}
// 12.12.12.100:30002/docker/sec-gateway-service