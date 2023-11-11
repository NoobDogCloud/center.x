package model;

import common.java.Database.DBLayer;
import common.java.Rpc.FilterReturn;
import main.java.Api.Configs;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

public class ConfigTemplateModel {
    public static FilterReturn deleteConfigTemplate(String[] idArr) {
        // 获得当前模板ID在configs中使用量
        Configs cfg = new Configs();
        DBLayer db = cfg.getDb().or();
        for (String id : idArr) {
            db.eq("templateid", id);
        }
        JSONArray<JSONObject> result = JSONArray.build();
        if (!db.nullCondition()) {
            result = db.select();
        }
        return result.size() > 0 ? FilterReturn.build(false, "当前模板正在使用,无法删除") : FilterReturn.success();
    }
}
