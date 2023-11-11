package main.java.Main;

import common.java.String.StringHelper;
import io.fabric8.kubernetes.api.model.Service;
import main.java.Api.Apps;
import main.java.Api.ServicesDeploy;
import main.java.Module.AppsBind;
import main.java.Module.ServicesBind;
import main.java.Module.ServicesDeployBind;
import org.json.gsc.JSONObject;

import java.util.List;

public class InitMaster {
    public static void Init() {
        // 初始化 k8s 命名空间
        InitApp();

        // 初始化 service And pod 容器
        InitService();
    }

    private static void InitApp() {
        var db = (new Apps()).getDb();
        var svr_db = (new ServicesDeploy()).getDb();
        var servicesBind = ServicesBind.build();
        var servicesDeployBind = ServicesDeployBind.build();
        var appsBind = AppsBind.build();
        db.scan(resultBlock -> {
            for (JSONObject block : resultBlock) {
                var appId = block.getString("id");
                appsBind.create(block);
                // 删除多余的服务
                List<Service> svr_arr = servicesBind.select(block);
                if (svr_arr != null) {
                    for (var svr : svr_arr) {
                        var deploymentName = ServicesBind.getServiceSystemName(svr.getMetadata().getName());
                        if (!StringHelper.isInvalided(deploymentName)) {
                            JSONObject result = svr_db.eq("name", deploymentName).eq("appid", appId).eq("container", 1).find();
                            if (JSONObject.isInvalided(result)) {
                                servicesDeployBind.delete(appId, deploymentName, block);
                            }
                        }
                    }
                }
            }
            return null;
        }, 50);
    }

    private static void InitService() {
        // 为每个部署维持一个环境
        var db = (new ServicesDeploy()).getDb();
        var servicesDeployBind = ServicesDeployBind.build();
        db.eq("container", 1).scan(resultBlock -> {
            for (JSONObject block : resultBlock) {
                servicesDeployBind.create(block);
            }
            return null;
        }, 50);
    }
}
