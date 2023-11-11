package model;

import common.java.Check.CheckHelper;
import main.java.Api.Cluster;
import org.json.gsc.JSONObject;

public class ClusterModel {
    private final JSONObject k8sInfo;

    private ClusterModel(int k8sId) {
        var k8sDb = (new Cluster()).getDb();
        k8sInfo = k8sDb.eq(k8sDb.getGeneratedKeys(), k8sId).find();
    }

    public static boolean check_k8s(int k8sId) {
        // 集群Id为0,则不需要检查
        if (k8sId == 0) {
            return true;
        }
        Cluster k8s = new Cluster();
        long no = k8s.getDb().eq("id", k8sId).count();
        return no > 0;
    }

    public static boolean check_k8s_registry(JSONObject registryInfo) {
        // 不设置 私有仓库
        if (JSONObject.isInvalided(registryInfo)) {
            return true;
        }
        return registryInfo.containsKey("domain") &&
                registryInfo.containsKey("port") &&
                registryInfo.containsKey("id") &&
                registryInfo.containsKey("password");
    }

    public static boolean check_name(String name) {
        return CheckHelper.IsStrictID(name, 64) &&
                !name.equals("default") &&
                !name.equals("system") &&
                !name.startsWith("kube-");
    }

    public static ClusterModel build(int k8sId) {
        return new ClusterModel(k8sId);
    }

    public String getConfig() {
        return k8sInfo != null ? k8sInfo.getString("config") : null;
    }
}
