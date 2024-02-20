package main.java.Module;

import common.java.String.StringHelper;
import db.service.BaseTemplate;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.KubernetesClient;
import model.deploySystem.KubernetesHandle;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

import java.util.List;

public class ServicesBind {
    private KubernetesClient client;

    private ServicesBind() {
    }


    public static ServicesBind build() {
        return new ServicesBind();
    }

    public static JSONObject getInfo(int id) {
        var db = (new BaseTemplate("services")).getDb();;
        return db.eq(db.getGeneratedKeys(), id).find();
    }

    public boolean update(int id, JSONObject newInfo) {
        JSONObject info = getInfo(id);
        if (JSONObject.isInvalided(info)) {
            return false;
        }
        JSONArray<String> keys = info.getNeField(newInfo);
        if (keys.isEmpty()) {
            return true;
        }
        // 修改 service 敏感字段会触发 service 的更新
        if (!keys.contains("dockerimage") &&
                !keys.contains("version") &&
                !keys.contains("port") &&
                !keys.contains("open")
        ) {
            return true;
        }
        info.put(newInfo);
        // 获得所有该服务的部署
        var deploys = ServicesDeployBind.getInfoByServiceID(id);
        if (!deploys.isEmpty()) {
            var servicesDeployBind = ServicesDeployBind.build();
            for (var deploy : deploys) {
                if (keys.contains("dockerimage") || keys.contains("version")) {
                    servicesDeployBind.upgrade(deploy.getInt("id"), newInfo.getString("dockerimage"), newInfo.getString("version"));
                }
                if (keys.contains("port") || keys.contains("open")) {
                    servicesDeployBind.update(deploy.getInt("id"), JSONObject.build(), null, info);
                }
            }
        }
        return true;
    }

    public static String getServiceSystemName(String k8sName) {
        var arr = k8sName.split("-");
        return StringHelper.join(arr, "-", 0, arr.length - 1);
    }

    private JSONObject getClient(int id) {
        JSONObject clusterInfo = ClusterBind.getInfo(id);
        if (JSONObject.isInvalided(clusterInfo)) {
            return null;
        }
        client = KubernetesHandle.getInstance(clusterInfo.getString("config"));
        if (client == null) {
            return null;
        }
        return clusterInfo;
    }

    // 获得当前应用全部服务信息
    public List<Service> select(JSONObject appInfo) {
        if (JSONObject.isInvalided(appInfo)) {
            return null;
        }
        int clusterId = appInfo.getInt("k8s");
        if (clusterId == 0) {
            return null;
        }
        getClient(clusterId);
        if (client == null) {
            return null;
        }
        String nameSpace = appInfo.getString("name");
        return client.services().inNamespace(nameSpace).list().getItems();
    }
}
