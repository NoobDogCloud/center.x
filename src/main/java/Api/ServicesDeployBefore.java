package main.java.Api;

import common.java.GscCommon.CheckModel;
import common.java.Rpc.FilterReturn;
import common.java.Rpc.RpcBefore;
import common.java.Rpc.RpcJsonFilterHelper;
import common.java.String.StringHelper;
import common.java.Time.TimeHelper;
import model.ClusterModel;
import model.ConfigModel;
import model.ServiceDeployModel;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;


public class ServicesDeployBefore extends RpcBefore {
    public ServicesDeployBefore() {
        input((serviceDeploy, ids) -> {
            ServicesDeploy deploy = new ServicesDeploy();
            String pk = deploy.getDb().getGeneratedKeys();
            boolean isUpdate = ids != null;
            FilterReturn r = RpcJsonFilterHelper.build(serviceDeploy, isUpdate)
                    .filter("appid", (info, name) -> ServiceDeployModel.check_appId(info.getString(name)) ? FilterReturn.success() : FilterReturn.fail("所属应用不合法"),
                            true,
                            "未定义所属应用")
                    .filter("serviceid", (info, name) -> ServiceDeployModel.check_serviceId(info.get(name)) ? FilterReturn.success() : FilterReturn.fail("所属服务不合法"),
                            true,
                            "未定义所属服务")
                    .filter("config", (info, name) -> ConfigModel.check_config(info.getJson(name)) ? FilterReturn.success() : FilterReturn.fail("配置不合法"),
                            false,
                            "未定义配置")
                    .filter("name", (info, name) -> ClusterModel.check_name(info.getString(name)) ? FilterReturn.success() : FilterReturn.fail("无效服务部署名称"),
                            false,
                            "未定义配置")
                    .filter("proxy_target", (info, name) -> {
                                var appId = info.getString("appid");
                                var target_arr = info.<String>getJsonArray("proxy_target");
                                JSONArray<String> validArr = ServiceDeployModel.check_proxy(appId, target_arr);
                                return validArr.size() == target_arr.size() ?
                                        FilterReturn.success() :
                                        FilterReturn.fail("无效代理服务" + validArr);
                            },
                            false,
                            "不合法的代理服务目标")
                    .filterUnique("name", pk, (line) -> deploy.getDb().eq("name", line.getString("name")).eq("appid", line.getString("appid")).limit(2).field(new String[]{pk}).select())
                    .check().getResult();
            if (!r.isSuccess()) {
                return r;
            }
            var nt = TimeHelper.build().nowDatetime();
            if (isUpdate) {
                if (ids.length != 1) {
                    return FilterReturn.fail("只能更新一条数据");
                }
                serviceDeploy.put("updateat", nt);
            }

            // 补充默认字段值
            serviceDeploy.put("createat", nt).put("state", CheckModel.active);
            Services services = new Services();
            var serviceInfo = services.getDb().eq("id", serviceDeploy.getInt("serviceid")).find();
            serviceDeploy.put("version", serviceInfo.getString("version"));
            return FilterReturn.success();
        }).delete((ids) -> {
            var deployDb = (new ServicesDeploy()).getDb();
            for (var id : ids) {
                JSONObject deployInfo = deployDb.eq("id", id).find();
                if (!JSONObject.isInvalided(deployInfo)) {
                    var deploy_name = deployInfo.getString("name");
                    // 包含当前服务的服务
                    var hasArr = deployDb.like("proxy_target", deploy_name).scan((arr) -> {
                        var result = JSONArray.<JSONObject>build();
                        for (JSONObject v : arr) {
                            var proxyTargetArr = v.<String>getJsonArray("proxy_target");
                            if (proxyTargetArr.contains(deploy_name)) {
                                result.put(v);
                            }
                        }
                        return result;
                    }, 50);
                    if (hasArr.size() > 0) {
                        return FilterReturn.fail("当前服务被[" + StringHelper.join(hasArr) + "]继承,无法删除");
                    }
                }
            }
            return FilterReturn.success();
        });
    }
}
