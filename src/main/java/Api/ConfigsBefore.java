package main.java.Api;

import common.java.Rpc.FilterReturn;
import common.java.Rpc.RpcBefore;
import common.java.Rpc.RpcJsonFilterHelper;
import common.java.Time.TimeHelper;
import model.ConfigModel;

public class ConfigsBefore extends RpcBefore {
    public ConfigsBefore() {
        input((config, ids) -> {
            boolean isUpdate = ids != null;
            FilterReturn r = RpcJsonFilterHelper.build(config, isUpdate)
                    .filter("type", (info, name) ->
                                    ConfigModel.check_type(info.getString(name)) ? FilterReturn.success() : FilterReturn.fail("未知配置类型"),
                            true,
                            "类型信息未提供")
                    .filter("templateid", (info, name) -> ConfigModel.checkTemplate(info),
                            false,
                            "模板信息未提供")
                    .check()
                    .getResult();
            if (!r.isSuccess()) {
                return r;
            }
            if (!ConfigModel.filterConfig(config)) {
                return FilterReturn.build(false, "配置信息不合法");
            }
            var nt = TimeHelper.build().nowDatetime();
            if (!isUpdate) {
                config.put("createat", nt);
            }
            config.put("updateat", nt);
            return FilterReturn.success();
        }).delete(ConfigModel::deleteConfig);
    }
}
