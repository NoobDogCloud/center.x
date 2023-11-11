package main.java.Module;

import common.java.GscCommon.CheckModel;
import common.java.Time.TimeHelper;
import common.java.nLogger.nLogger;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import main.java.Api.Apps;
import main.java.Api.Services;
import main.java.Api.ServicesDeploy;
import model.deploySystem.KubeSecretDockerConfigJson;
import model.deploySystem.KubernetesHandle;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

import java.util.Collections;

public class AppsBind {

    private String errorMessage = "成功";
    private KubernetesClient client;

    private AppsBind() {
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public static AppsBind build() {
        return new AppsBind();
    }

    public static JSONObject getInfo(String id) {
        var db = (new Apps()).getDb();
        return db.eq(db.getGeneratedKeys(), id).find();
    }

    public static JSONArray<JSONObject> getInfoByClusterId(int clusterId) {
        var db = (new Apps()).getDb();
        return db.eq("k8s", clusterId).select();
    }

    private JSONObject getClient(int k8sId) {
        JSONObject clusterInfo = ClusterBind.getInfo(k8sId);
        if (JSONObject.isInvalided(clusterInfo)) {
            return null;
        }
        client = KubernetesHandle.getInstance(clusterInfo.getString("config"));
        if (client == null) {
            return null;
        }
        return clusterInfo;
    }

    public boolean create(JSONObject appInfo) {
        String nameSpace = appInfo.getString("name");
        int clusterId = appInfo.getInt("k8s");
        if (clusterId == 0) {
            errorMessage = "应用未设置部署集群";
            return true;
        }
        JSONObject clusterInfo = getClient(clusterId);
        if (JSONObject.isInvalided(clusterInfo)) {
            errorMessage = "应用集群信息无效";
            return false;
        }
        var r_ns = client.namespaces().withName(nameSpace);
        if (r_ns.get() == null) {
            Namespace ns = new NamespaceBuilder().withNewMetadata().withName(nameSpace).endMetadata().build();
            r_ns.create(ns);
        }
        // 导入私有镜像仓库配置
        return this.updateRegistry(clusterInfo, nameSpace);
    }

    private boolean delete(String nameSpace, int k8sId, String appId) {
        if (k8sId == 0) {
            return true;
        }
        getClient(k8sId);
        if (client == null) {
            return false;
        }
        // 删除可能包含的 ingress
        // cancelPublisher(k8sId, nameSpace);
        // 删除命名空间内所有部署和服务
        var deploys = ServicesDeployBind.getInfoByAppID(appId);
        if (deploys.size() > 0) {
            var servicesDeployBind = ServicesDeployBind.build();
            for (var deploy : deploys) {
                servicesDeployBind.delete(deploy.getString("appid"), deploy.getString("name"));
            }
        }
        // 删除私有镜像仓库配置
        this.deleteRegistry(nameSpace);
        // 删除命名空间
        var r_ns = client.namespaces().withName(nameSpace);
        if (r_ns.get() != null) {
            r_ns.delete();
        }
        return true;
    }

    public boolean delete(String id) {
        JSONObject appInfo = getInfo(id);
        if (JSONObject.isInvalided(appInfo)) {
            return false;
        }
        String nameSpace = appInfo.getString("name");
        int clusterId = appInfo.getInt("k8s");
        return delete(nameSpace, clusterId, id);
    }

    public boolean update(String id, JSONObject newInfo) {
        return update(id, newInfo, JSONArray.build());
    }

    public boolean update(String id, JSONObject newInfo, JSONArray<String> modifyKeys) {
        JSONObject appInfo = getInfo(id);
        if (JSONObject.isInvalided(appInfo)) {
            return false;
        }
        JSONArray<String> keys = appInfo.getNeField(newInfo).put(modifyKeys);
        if (keys.size() == 0) {
            return true;
        }
        // 记录旧的名称
        String currentNameSpace = appInfo.getString("name");
        int currentClusterId = appInfo.getInt("k8s");
        String currentCategory = appInfo.getString("category");
        // 创建新的
        appInfo.put(newInfo);
        if (!create(appInfo)) {
            return false;
        }
        // 修改 app 敏感字段会触发 app 的 service 更新
        if (!keys.contains("k8s") &&
                !keys.contains("name") &&
                !keys.contains("category") &&
                !keys.contains("master") &&
                !keys.contains("secret")
        ) {
            return true;
        }
        var deploys = ServicesDeployBind.getInfoByAppID(id);
        if (deploys.size() > 0) {
            var servicesDeployBind = ServicesDeployBind.build();
            for (var deploy : deploys) {
                servicesDeployBind.update(deploy.getInt("id"), JSONObject.build(), appInfo, null);
            }
        }
        // 发布应用到 ingress(基于域名判断是否使用 ingress 发布应用)->已废弃
        /*
        if (keys.contains("domain")) {
            String domain = newInfo.getString("domain");
            if (StringHelper.isInvalided(domain)) {
                // 删除 ingress
                cancelPublisher(currentClusterId, currentNameSpace);
            } else {
                publisher(appInfo);
            }
        }
        */
        if (keys.contains("category") && !currentCategory.equals(newInfo.getString("category"))) {
            // 重新发布
            gatewayInfo(appInfo);
        }

        // 删除旧的 namespace
        if (keys.contains("name") && !currentNameSpace.equals(newInfo.getString("name"))) {
            return delete(currentNameSpace, currentClusterId, id);
        }
        return true;
    }

    public boolean updateRegistry(JSONObject clusterInfo, String nameSpace) {
        if (client == null) {
            errorMessage = "应用部署集群无法连接";
            return false;
        }
        JSONObject registryInfo = clusterInfo.getJson("registry");
        if (JSONObject.isInvalided(registryInfo)) {
            errorMessage = "集群未设置镜像仓库";
            return false;
        }
        var secret = client.secrets().inNamespace(nameSpace).withName(nameSpace + "-registry").get();
        if (secret == null) {
            var registrySecret = KubeSecretDockerConfigJson.build(registryInfo).build();
            secret = new SecretBuilder().withNewMetadata()
                    .withName(nameSpace + "-registry")
                    .endMetadata()
                    .withType(KubeSecretDockerConfigJson.getType())
                    .withImmutable(false)
                    .addToData(registrySecret)
                    .build();
        }
        client.secrets().inNamespace(nameSpace).createOrReplace(secret);
        return true;
    }

    public boolean deleteRegistry(String nameSpace) {
        if (client == null) {
            errorMessage = "应用部署集群无法连接";
            return false;
        }
        var secretIns = client.secrets().inNamespace(nameSpace).withName(nameSpace + "-registry");
        if (secretIns.get() == null) {
            secretIns.delete();
        }
        return true;
    }

    /*
    public boolean publisher(String id) {
        return publisher(getInfo(id));
    }

    public boolean publisher(JSONObject appInfo) {
        int clusterId = appInfo.getInt("k8s");
        if (clusterId == 0) {
            return true;
        }
        String domains = appInfo.getString("domain");
        if (StringHelper.isInvalided(domains)) {
            return false;
        }
        String[] domainsArray = domains.split(",");
        getClient(clusterId);
        if (client == null) {
            return false;
        }
        String nameSpace = appInfo.getString("name");
        // var ingressIns = client.network().v1().ingresses().inNamespace(nameSpace).withName(nameSpace + "-ingress");
        String appId = appInfo.getString("id");
        var serviceDb = (new Services()).getDb();
        var deployDb = (new ServicesDeploy()).getDb();
        // 构造 ingress path
        HTTPIngressRuleValueBuilder httpIngressRuleValueBuilder = new HTTPIngressRuleValueBuilder();
        deployDb.eq("appid", appId).scan(record -> {
            for (var item : record) {
                var deployName = item.getString("name");
                var serviceId = item.getInt("serviceid");
                var serviceInfo = serviceDb.eq(serviceDb.getGeneratedKeys(), serviceId).find();
                if (JSONObject.isInvalided(serviceInfo)) {
                    continue;
                }

                var port = serviceInfo.getInt("port");
                httpIngressRuleValueBuilder.addNewPath()
                        .withPath("/" + item.getString("name") + "/")
                        .withPathType("Prefix")
                        .withNewBackend()
                        .withService(
                                new IngressServiceBackend(deployName + "-service",
                                        new ServiceBackendPortBuilder().withNumber(port).build()
                                )
                        )
                        .endBackend()
                        .endPath();
            }
            return null;
        }, 50);
        var ruleValue = httpIngressRuleValueBuilder.build();

        var ingressBuilder = new IngressBuilder()
                .withNewMetadata()
                .withName(nameSpace + "-ingress")
                .withNamespace(nameSpace)
                .endMetadata()
                .withNewSpec()
                .withIngressClassName("nginx")
                .addNewRule();
        for (String s : domainsArray) {
            String[] domainInfo = s.split(":");
            String domain = domainInfo[0];
            // int port = domainInfo.length == 1 ? 80 : Integer.parseInt(domainInfo[1]);
            ingressBuilder.withHost(domain).withHttp(ruleValue);
        }

        var ingressTpl = ingressBuilder.endRule()
                .endSpec()
                .build();
        client.network().v1().ingresses().inNamespace(nameSpace).withName(nameSpace + "-ingress").createOrReplace(ingressTpl);
        return true;
    }

    public boolean cancelPublisher(String id) {
        JSONObject appInfo = getInfo(id);
        return cancelPublisher(appInfo.getInt("k8s"), appInfo.getString("name"));
    }

    public boolean cancelPublisher(int clusterId, String nameSpace) {
        getClient(clusterId);
        if (client == null) {
            return false;
        }
        var ingressIns = client.network().v1().ingresses().inNamespace(nameSpace).withName(nameSpace + "-ingress");
        ingressIns.delete();
        return true;
    }
     */

    /**
     * @apiNote ingress 版本获得gateway的访问地址
     */
    /*
    public JSONObject gatewayInfo(String id) {
        JSONObject appInfo = getInfo(id);
        return gatewayInfo(appInfo);
    }

    public JSONObject gatewayInfo(JSONObject appInfo) {
        String nameSpace = appInfo.getString("name");
        int clusterId = appInfo.getInt("k8s");
        getClient(clusterId);
        if (client == null) {
            return null;
        }
        var ingress = client.network().v1().ingresses().inNamespace(nameSpace).withName(nameSpace + "-ingress").get();
        if (ingress == null) {
            return null;
        }
        JSONObject result = new JSONObject();
        ingress.getSpec().getRules().forEach(rule -> {
            if (rule.getHost() != null) {
                JSONObject pathInfo = JSONObject.build();
                var paths = rule.getHttp().getPaths();
                paths.forEach(path -> pathInfo.put(path.getPath(), path.getBackend().getService().getName()));
                result.put(rule.getHost(), pathInfo);
            }
        });
        return result;
    }
     */

    /**
     * @return 网关服务连接信息
     * @apiNote 葡萄应用引擎版获得gateway的访问地址(包含重发布功能)
     * 支持3种网关模式:
     * 1:普通网关:通过 gateway-service#node-service 提供 /api/xxx 代理所有请求
     * 2:网格网格 通过网格服务重定向到应用入口,应用可以运行直接访问各微服务node-service
     * 3:安全网关 通过安全网关重定向到应用入口,提供 /api/xxx 加密方式代理所有请求,根据配置支持api混合加密/非加密请求
     */
    public JSONObject gatewayInfo(String id) {
        JSONObject appInfo = getInfo(id);
        return gatewayInfo(appInfo);
    }

    public JSONObject gatewayInfo(JSONObject appInfo) {
        int clusterId = appInfo.getInt("k8s");
        getClient(clusterId);
        if (client == null) {
            nLogger.errorInfo("k8s集群不存在或未设置");
            return null;
        }
        // 获得发布模式(对应模式服务名称)
        String publishMode = appInfo.getString("category");
        ServicesDeployBind sdb = ServicesDeployBind.build();
        var s = new Services();
        int serviceId = -1;
        JSONArray nodeInfo = JSONArray.build();
        // 查看是否已存在发布服务部署
        var d = new ServicesDeploy();
        var d_db = d.getDb();
        String appId = appInfo.getString("id");
        // 有效的网关服务名称
        if (publishMode.length() > 0) {
            JSONObject serviceInfo = s.find("name", publishMode);
            if (JSONObject.isInvalided(serviceInfo)) {
                // 发布服务不存在
                nLogger.errorInfo("发布服务不存在");
                return null;
            }
            serviceId = serviceInfo.getInt("id");
            // 获得获得对应发布网关服务信息
            JSONObject deployInfo;
            do {
                deployInfo = d_db.eq("appid", appId).eq("serviceid", serviceId).find();
                if (JSONObject.isInvalided(deployInfo)) {
                    // 动态部署对应服务
                    JSONObject deployGateway = JSONObject.build()
                            .put("appid", appId)
                            .put("serviceid", serviceId)
                            .put("debug", serviceInfo.getInt("debug"))
                            .put("config", serviceInfo.get("config"))
                            .put("datamodel", serviceInfo.get("datamodel"))
                            .put("version", serviceInfo.get("version"))
                            .put("name", publishMode)
                            .put("state", CheckModel.active)
                            .put("updateat", TimeHelper.build().nowDatetime())
                            .put("replicaset", 1)
                            .put("secure", 0)
                            .put("open", 1)
                            .put("clusteraddr", "")
                            .put("proxy_target", JSONArray.<String>build())
                            .put("subaddr", "");
                    if (!(d.insert(deployGateway) instanceof Boolean)) {
                        // 动态部署失败
                        errorMessage = "动态部署服务失败";
                        return null;
                    }
                }
            } while (JSONObject.isInvalided(deployInfo));
            // 获得部署信息,通过部署信息获得对应服务的访问node或者lb信息
            if (!sdb.updateEndpoint(deployInfo, appInfo)) {
                errorMessage = "获得部署服务连接信息失败";
                return null;
            }
            var pkv = deployInfo.get(d.getDb().getGeneratedKeys());
            JSONObject result = d.find(pkv);
            if (JSONObject.isInvalided(result)) {
                errorMessage = "获得新部署服务异常";
                return null;
            }
            String[] nodeInfoArr = result.getString("subaddr").split(",");
            Collections.addAll(nodeInfo, nodeInfoArr);
        }

        // 删除原有网关服务(使用无效的网关会导致删除全部网关服务)
        int finalServiceId = serviceId;
        var api_sdp = new ServicesDeploy();
        s.getDb().eq("category", "gateway").scan(arr -> {
            for (JSONObject item : arr) {
                int sId = item.getInt("id");
                if (sId != finalServiceId) {
                    // 获得当前应用对应网关的部署实例
                    JSONObject deployInfo = d_db.eq("appid", appId).eq("serviceid", sId).find();
                    if (!JSONObject.isInvalided(deployInfo)) {
                        String deployId = deployInfo.getString("id");
                        api_sdp.delete(deployId);
                    }
                }
            }
            return null;
        }, 50);
        // 构造返回结构
        return JSONObject.build().put("subaddr", nodeInfo).put("category", publishMode);
    }

    private JSONObject getServiceDeployInfo(String appPublishMode) {
        JSONObject r = JSONObject.build();
        switch (appPublishMode) {
            case "no-publish":
            case "gateway-service": {
                r.put("open", 0);
                break;
            }
            case "secgateway-service": {
                r.put("open", 0);
                r.put("secure", 1);
                break;
            }
            case "node-service": {
                r.put("open", 1);
                break;
            }
        }
        return r;
    }

    // 为应用维持指定的服务
    public boolean keepUserService(JSONObject appInfo) {
        int clusterId = appInfo.getInt("k8s");
        getClient(clusterId);
        if (client == null) {
            errorMessage = "k8s集群不存在或未设置";
            return true;
        }
        String appId = appInfo.getString("appid");
        // 用户未设置用户服务
        var user_service_id = appInfo.getInt("user_service_id");
        if (user_service_id == 0) {
            return true;
        }
        var service = new Services();
        var serviceDb = service.getDb();
        // 判断应用绑定的服务是否有效
        JSONObject serviceInfo = serviceDb.eq("id", user_service_id).find();
        if (JSONObject.isInvalided(serviceInfo)) {
            errorMessage = "用户服务不存在";
            return false;
        }

        String user_service_name = serviceInfo.getString("name") + "-" + "user-service";
        var servicesDeploy = new ServicesDeploy();
        var servicesDeployDb = servicesDeploy.getDb();
        // 判断当前服务部署实例是否已存在
        JSONObject deployInfo = servicesDeployDb.eq("name", user_service_name).eq("appid", appId).find();
        if (JSONObject.isInvalided(deployInfo)) {
            String appCategory = appInfo.getString("category");
            JSONObject servicePreInfo = getServiceDeployInfo(appCategory);
            // 动态部署对应服务
            JSONObject deployUser = JSONObject.build()
                    .put("appid", appId)
                    .put("serviceid", user_service_id)
                    .put("debug", serviceInfo.getInt("debug"))
                    .put("config", serviceInfo.get("config"))
                    .put("datamodel", serviceInfo.get("datamodel"))
                    .put("version", serviceInfo.get("version"))
                    .put("name", user_service_name)
                    .put("state", CheckModel.active)
                    .put("updateat", TimeHelper.build().nowDatetime())
                    .put("replicaset", 1)
                    .put("secure", 0)
                    .put("open", 1)
                    .put("clusteraddr", "")
                    .put("proxy_target", JSONArray.<String>build())
                    .put("subaddr", "");
            deployUser.put(servicePreInfo);
            if (!(servicesDeploy.insert(deployUser) instanceof Boolean)) {
                // 动态部署失败
                nLogger.errorInfo("动态绑定用户服务失败");
                return false;
            }
        }
        return true;
    }
}
