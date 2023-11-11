package main.java.Api;

import common.java.Database.DBLayer;
import common.java.Rpc.RpcMessage;
import common.java.String.StringHelper;
import db.service.ApplicationTemplate;
import main.java.Module.ServicesDeployBind;
import model.ConfigModel;
import org.json.gsc.JSONObject;

import java.util.HashSet;
import java.util.Set;

public class ServicesDeploy extends ApplicationTemplate {
    /*
    private static final ExecutorService Worker = Executors.newSingleThreadExecutor();
    static {
        Worker.execute(() -> {
            // 为每个部署维持一个环境
            var db = (new ServicesDeploy()).getDb();
            var servicesDeployBind = ServicesDeployBind.build();
            db.scan(resultBlock -> {
                for (JSONObject block : resultBlock) {
                    servicesDeployBind.create(block);
                }
                return null;
            }, 50);
        });
    }
    */

    public ServicesDeploy() {
        super("servicesDeploy");
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
        var bind = ServicesDeployBind.build();
        int container = nObj.getInt("container");
        if (container == 1) {
            if (!super.insertOrRollback(nObj, (id) -> bind.create(nObj))) {
                // 更新服务访问信息到 services
                return RpcMessage.Instant(1, bind.getErrorMessage());
            }
        }
        return true;
    }

    public boolean update(String uids, JSONObject info) {
        var idArr = uids.split(",");
        if (idArr.length == 0) {
            return false;
        }
        int container = info.getInt("container");
        int id = Integer.parseInt(idArr[0]);
        if (container == 1) {
            if (!ServicesDeployBind.build().update(id, info)) {
                return false;
            }
        } else {
            ServicesDeployBind.build().delete(id);
        }
        return super.update(uids, info);
    }

    public boolean delete(String uids) {
        var idArr = uids.split(",");
        if (idArr.length == 0) {
            return false;
        }
        ServicesDeployBind.build().delete(Integer.parseInt(idArr[0]));
        return super.delete(uids);
    }

    // 获得 pod 的状态
    public JSONObject podStatus(String uids) {
        var idArr = uids.split(",");
        if (idArr.length == 0) {
            return JSONObject.build();
        }
        return ServicesDeployBind.build().getPodsStatus(Integer.parseInt(idArr[0]));
    }
}
