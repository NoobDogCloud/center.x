package main.java.Api;

import common.java.GscCommon.CheckModel;
import common.java.Rpc.FilterReturn;
import common.java.Rpc.RpcBefore;
import common.java.Rpc.RpcJsonFilterHelper;
import common.java.String.StringHelper;
import common.java.Time.TimeHelper;
import model.AppModel;
import model.ClusterModel;
import model.ConfigModel;


public class AppsBefore extends RpcBefore {
    public AppsBefore() {
        input((app, ids) -> {
            boolean isUpdate = ids != null;
            var api = new Apps();
            String pk = api.getDb().getGeneratedKeys();
            FilterReturn r = RpcJsonFilterHelper.build(app, isUpdate)
                    // 检查配置信息
                    .filter("config", (info, name) ->
                                    ConfigModel.check_config(app.getJson("config")) ? FilterReturn.success() : FilterReturn.fail("配置信息无效"),
                            true,
                            "配置信息未提供")
                    .filter("k8s", (info, name) ->
                                    ClusterModel.check_k8s(info.getInt("k8s")) ? FilterReturn.success() : FilterReturn.fail("k8s集群信息无效"),
                            false,
                            "k8s信息未提供")
                    .filter("name", (info, name) -> AppModel.check_name(info.getString(name)) ? FilterReturn.success() : FilterReturn.fail("无效应用名称"))
                    .filterUnique("name", pk, (line) -> api.getDb().limit(2).field(new String[]{pk}).eq("name", line.getString("name")).select())
                    .check()
                    .getResult();
            if (!r.isSuccess()) {
                return r;
            }
            // 创建时 补充默认值
            var nt = TimeHelper.build().nowDatetime();
            if (!isUpdate) {
                // 默认信息
                app.put("state", CheckModel.active)
                        .put("createat", nt)
                        .put("roles", "");
                String secret = app.getString("secret");
                if (StringHelper.isInvalided(secret)) {
                    app.put("secret", StringHelper.randomString(64));
                }
                String devSecret = app.getString("dev_secret");
                if (StringHelper.isInvalided(devSecret)) {
                    app.put("dev_secret", StringHelper.randomString(64));
                }
            } else {
                if (ids.length != 1) {
                    return FilterReturn.fail("只能更新一个应用");
                }
            }
            // 维持用户服务
            if (!AppModel.freshUserService(app)) {
                return FilterReturn.fail("当前应用绑定的用户服务无效");
            }
            // 一定会补充的字段
            app.put("updateat", nt);
            return FilterReturn.success();
        }).delete(ids -> {
            // 判断应用是否包含部署
            var deployDb = (new ServicesDeploy()).getDb();
            for (var id : ids) {
                if (deployDb.eq("appid", id).count() > 0) {
                    return FilterReturn.fail("应用[" + id + "]包含已部署服务，无法删除");
                }
            }
            return FilterReturn.success();
        });
    }
}
