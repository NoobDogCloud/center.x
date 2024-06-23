package main.java.Api;

import common.java.Database.DBLayer;
import common.java.Random.Random;
import common.java.Rpc.RpcMessage;
import common.java.String.StringHelper;
import db.service.ApplicationTemplate;
import main.java.Module.AppsBind;
import model.ConfigModel;
import org.json.gsc.JSONObject;

import java.util.HashSet;
import java.util.Set;

public class Apps extends ApplicationTemplate {
    /*
    private static final ExecutorService Worker = Executors.newSingleThreadExecutor();

    static {
        Worker.execute(() -> {
            // 为每一个应用维持环境
            var db = (new Apps()).getDb();
            var appsBind = AppsBind.build();
            db.scan(resultBlock -> {
                for (JSONObject block : resultBlock) {
                    appsBind.create(block);
                }
                return null;
            }, 50);
        });
    }
    */
    public Apps() {
        super("apps");
    }

    public String getConfigName() {
        Set<String> cfgName = new HashSet<>();
        DBLayer db = getDb();
        db.scan(arr -> {
            for (JSONObject v : arr) {
                // 获得配置信息
                JSONObject configVal = ConfigModel.getConfigVal(v.getJson("config"));
                for (Object _val : configVal.values()) {
                    String val = (String) _val;
                    cfgName.add(val);
                }
            }
            return arr;
        }, 20);
        return StringHelper.join(cfgName);
    }

    public Object insert(JSONObject nObj) {
        // 生成唯一 appId
        String uniqueId;
        do {
            uniqueId = StringHelper.createRandomCode(2 + Random.getRandom(2, 6));
        } while (!JSONObject.isInvalided(getDb().eq("id", uniqueId).find()));
        nObj.put("id", uniqueId);
        // 创建应用
        var appBind = AppsBind.build();
        if (!appBind.create(nObj)) {
            return RpcMessage.Instant(1, appBind.getErrorMessage());
        }
        var r = super.insert(nObj);
        // 发布应用
        if( !nObj.getString("category").equals("no-publish") ){
            if (r instanceof Boolean) {
                appBind.gatewayInfo(nObj);
            }
        }
        return r;
    }

    public boolean update(String uids, JSONObject info) {
        var idArr = uids.split(",");
        if (idArr.length == 0) {
            return false;
        }
        if (!AppsBind.build().update(idArr[0], info)) {
            return false;
        }
        return super.update(uids, info);
    }

    public boolean delete(String uids) {
        var idArr = uids.split(",");
        if (idArr.length == 0) {
            return false;
        }
        if (!AppsBind.build().delete(idArr[0])) {
            return false;
        }
        return super.delete(uids);
    }

    public JSONObject gateway(String uids) {
        JSONObject result = JSONObject.build();
        var idArr = uids.split(",");
        if (idArr.length == 0) {
            return result;
        }
        var ab = AppsBind.build();
        for (String id : idArr) {
            var info = ab.gatewayInfo(id);
            if (info != null) {
                result.put(id, info);
            }
        }
        return result;
    }
}
