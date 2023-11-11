package main.java.Api;

import common.java.Rpc.FilterReturn;
import common.java.Rpc.RpcBefore;
import common.java.Rpc.RpcJsonFilterHelper;
import common.java.Time.TimeHelper;
import model.ConfigModel;
import model.ConfigTemplateModel;
import org.json.gsc.JSONObject;


public class ConfigTemplatesBefore extends RpcBefore {
    public ConfigTemplatesBefore() {
        input((configTemplate, ids) -> {
            boolean isUpdate = ids != null;
            FilterReturn r = RpcJsonFilterHelper.build(configTemplate, isUpdate)
                    .filter("type", (info, name) ->
                                    ConfigModel.check_type(info.getString(name)) ? FilterReturn.success() : FilterReturn.fail("无效配置分类"),
                            true,
                            "配置分类未设置")
                    .filter("template", (info, name) -> JSONObject.isInvalided(info.getJson(name)) ? FilterReturn.fail("无效配置模板") : FilterReturn.success(),
                            true,
                            "配置模板未设置")
                    .check()
                    .getResult();
            if (!r.isSuccess()) {
                return r;
            }
            var nt = TimeHelper.build().nowDatetime();
            if (!isUpdate) {
                configTemplate.put("createat", nt);
            }
            configTemplate.put("updateat", nt);
            return FilterReturn.success();
        }).delete(ConfigTemplateModel::deleteConfigTemplate);
    }
}
