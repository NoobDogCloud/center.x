package main.java.Module;

import common.java.String.StringHelper;
import common.java.Thread.ThreadHelper;
import common.java.Time.TimerHelper;
import common.java.nLogger.nLogger;
import db.service.BaseTemplate;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import model.deploySystem.KubernetesHandle;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ServicesDeployBind {
    private String errorMessage = "成功";
    private KubernetesClient client;

    private ServicesDeployBind() {
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public static ServicesDeployBind build() {
        return new ServicesDeployBind();
    }

    public static JSONObject getInfo(int id) {
        var db = (new BaseTemplate("servicesDeploy")).getDb();
        return db.eq(db.getGeneratedKeys(), id).find();
    }

    public static JSONArray<JSONObject> getInfoByAppID(String id) {
        var db = (new BaseTemplate("servicesDeploy")).getDb();
        return db.eq("appid", id).select();
    }

    public static JSONArray<JSONObject> getInfoByServiceID(int id) {
        var db = (new BaseTemplate("servicesDeploy")).getDb();
        return db.eq("serviceid", id).select();
    }

    // 更新当前服务的外部访问信息
    public boolean updateEndpoint(JSONObject deployInfo, JSONObject appInfo) {
        JSONObject clusterInfo = getClient(appInfo.getInt("k8s"));
        if (JSONObject.isInvalided(clusterInfo)) {
            return false;
        }
        if (client == null) {
            nLogger.warnInfo("无效的集群授权文件");
            return false;
        }
        String nameSpace = appInfo.getString("name");
        String deploymentName = deployInfo.getString("name");
        int open_type = deployInfo.getInt("open");
        // 获得服务信息
        Service service = client.services().inNamespace(nameSpace).withName(deploymentName + "-service").get();
        if (service == null) {
            return false;
        }
        JSONObject updateInfo = new JSONObject();

        // 更新服务 cluster address(内部用)
        List<String> endpoints = new ArrayList<>();
        var ports = service.getSpec().getPorts();
        if (ports != null) {
            var cluster_ips = service.getSpec().getClusterIPs();
            if (cluster_ips != null) {
                for (var cluster_ip : cluster_ips) {
                    for (var port : ports) {
                        endpoints.add(cluster_ip + ":" + port.getPort());
                    }
                }
            }
        }
        updateInfo.put("clusteraddr", StringHelper.join(endpoints, ","));
        String[] ip_arr = clusterInfo.getString("nodeips").split(",");

        // 更新服务sub address(外部用)
        switch (open_type) {
            case 1 -> { // NodePort
                endpoints.clear();
                if (ports != null) {
                    for (var node_ip : ip_arr) {
                        for (var port : ports) {
                            var p = port.getNodePort();
                            if (p != null) {
                                endpoints.add(node_ip + ":" + p);
                            }
                        }
                    }
                }
                updateInfo.put("subaddr", StringHelper.join(endpoints, ","));
            }
            case 2 -> { // LoadBalancer
                endpoints.clear();
                var loadBalancer = service.getStatus().getLoadBalancer();
                if (loadBalancer != null) {
                    var ingresses = loadBalancer.getIngress();
                    if (ingresses != null) {
                        for (var ingress : ingresses) {
                            var ip = ingress.getIp();
                            var _ports = ingress.getPorts();
                            if (ports != null) {
                                for (var port : _ports) {
                                    var p = port.getPort();
                                    if (p != null) {
                                        endpoints.add(ip + ":" + port.getPort());
                                    }
                                }
                            }
                        }
                    }
                }
                updateInfo.put("subaddr", StringHelper.join(endpoints, ","));
            }
            default -> updateInfo.put("subaddr", "");
        }
        var deploy_db = (new BaseTemplate("servicesDeploy")).getDb();
        return deploy_db.eq("name", deployInfo.getString("name")).data(updateInfo).update();
    }

    private JSONObject getClient(int id) {
        JSONObject clusterInfo = ClusterBind.getInfo(id);
        if (JSONObject.isInvalided(clusterInfo)) {
            return null;
        }
        if (client == null) {
            client = KubernetesHandle.getInstance(clusterInfo.getString("config"));
            if (client == null) {
                return null;
            }
        }
        return clusterInfo;
    }

    public boolean create(JSONObject info) {
        return create(info, false);
    }

    public boolean create(JSONObject info, boolean update) {
        return create(info, update, null, null);
    }

    public boolean create(JSONObject info, boolean update, JSONObject appInfo, JSONObject serviceInfo) {
        if (appInfo == null) {
            appInfo = AppsBind.getInfo(info.getString("appid"));
        }
        if (JSONObject.isInvalided(appInfo)) {
            this.errorMessage = "无效的应用信息";
            return false;
        }
        if (serviceInfo == null) {
            serviceInfo = ServicesBind.getInfo(info.getInt("serviceid"));
        }
        if (JSONObject.isInvalided(serviceInfo)) {
            this.errorMessage = "无效的服务信息";
            return false;
        }
        int clusterId = appInfo.getInt("k8s");
        if (clusterId == 0) {
            this.errorMessage = "该应用未配置部署集群->已添加记录,但是未实际部署";
            return true;
        }
        JSONObject clusterInfo = getClient(clusterId);
        if (JSONObject.isInvalided(clusterInfo)) {
            this.errorMessage = "部署集群不存在";
            return false;
        }
        if (client == null) {
            this.errorMessage = "无效的集群授权文件";
            return false;
        }
        String dockerImage = serviceInfo.getString("dockerimage");
        if (StringHelper.isInvalided(dockerImage)) {
            this.errorMessage = "服务" + serviceInfo.getString("name") + "的镜像为空";
            return false;
        }
        String dockerVersion = serviceInfo.getString("version");
        if (StringHelper.isInvalided(dockerVersion)) {
            this.errorMessage = "服务" + serviceInfo.getString("name") + "的镜像版本为空";
            return false;
        }
        String dockerUrl = dockerImage + ":" + dockerVersion;
        int port = serviceInfo.getInt("port");
        String nameSpace = appInfo.getString("name");
        int scaleNum = Math.max(info.getInt("replicaset"), 1);
        String deploymentName = info.getString("name");
        var deploymentIns = client.apps().deployments().inNamespace(nameSpace).withName(deploymentName + "-deployment");
        var deployment = deploymentIns.get();
        if (deployment == null || update) {
            // 增加私有镜像服务器支持
            var deployBuilder = new DeploymentBuilder()
                    .withNewMetadata()
                    .withName(deploymentName + "-deployment")
                    .endMetadata()
                    .withNewSpec()
                    .withReplicas(scaleNum)
                    .withNewTemplate()
                    .withNewMetadata()
                    .addToLabels("app", nameSpace)
                    .addToLabels("service", deploymentName)
                    .endMetadata()
                    .withNewSpec();

            if (client.secrets().inNamespace(nameSpace).withName(nameSpace + "-registry").get() != null) {
                // 附加私有仓库
                deployBuilder = deployBuilder.addNewImagePullSecret().withName(nameSpace + "-registry").endImagePullSecret();
                // 附加Hosts
                JSONObject registryInfo = clusterInfo.getJson("registry");
                if (!JSONObject.isInvalided(registryInfo)) {
                    if (registryInfo.containsKey("ip")) {
                        deployBuilder = deployBuilder.addNewHostAlias()
                                .withHostnames(registryInfo.getString("domain"))
                                .withIp(registryInfo.getString("ip"))
                                .endHostAlias();
                    }
                }
            }

            var containerBuilder = deployBuilder.addNewContainer()
                    .withName(deploymentName)
                    .withImage(dockerUrl)
                    .withImagePullPolicy("Always")
                    .addNewPort()
                    .withContainerPort(port)
                    .endPort();
            // 附加启动参数 -k grapeSoft@ -p 805
            String masterUrl = appInfo.getString("master");
            if (StringHelper.isInvalided(masterUrl)) {
                this.errorMessage = "应用未填写主控服务器地址";
                return false;
            }
            int masterPort = Integer.parseInt(masterUrl.split(":")[1]);
            if (StringHelper.isInvalided(masterUrl)) {
                containerBuilder.withArgs("-k", appInfo.getString("secret"), "-p", StringHelper.toString(masterPort));
            } else {
                containerBuilder.withArgs(
                        "-n", deploymentName,
                        "-h", masterUrl,
                        "-a", appInfo.getString("id"),
                        "-p", StringHelper.toString(serviceInfo.getInt("port")),
                        "-k", appInfo.getString("secret"));
            }
            deployment = containerBuilder.endContainer()
                    .endSpec()
                    .endTemplate()
                    .withNewSelector()
                    .addToMatchLabels("app", nameSpace)
                    .addToMatchLabels("service", deploymentName)
                    .endSelector()
                    .endSpec()
                    .build();
            client.apps().deployments().inNamespace(nameSpace).createOrReplace(deployment);

            // 确定 指定 service 的 pod 状态与数量与记录一致
            deploymentIns.scale(scaleNum, true);
            if (!TimerHelper.schedule(() -> {
                var _deployment = deploymentIns.get();
                var st = _deployment != null ? _deployment.getStatus().getAvailableReplicas() : null;
                return (st != null && st == scaleNum);
            }, 1, 120)) {
                // 超时未完成
                this.errorMessage = "部署服务超时";
                return false;
            }
        }

        // 获得当前协议
        String serviceProtocol = serviceInfo.getString("protocol");

        var portsBuilder = new ServicePortBuilder()
                .withName(deploymentName + "open" + port)
                .withPort(port).withProtocol(serviceProtocol)
                .withTargetPort(new IntOrString(port));

        // 检查 service 对应 开放是否正常
        var serviceBuilder = new ServiceBuilder()
                .withNewMetadata()
                .withName(deploymentName + "-service")
                .endMetadata()
                .withNewSpec()
                .withSelector(JSONObject.build("app", nameSpace).put("service", deploymentName).toHashMap());
        int openType = info.getInt("open");
        switch (openType) {
            case 1 -> {
                serviceBuilder.withType("NodePort");
                // 补充固定 nodePort 设置
                int nodePort = info.getInt("target_port");
                if (nodePort > 0) {
                    portsBuilder.withNodePort(nodePort);
                }
            }
            case 2 -> serviceBuilder.withType("LoadBalancer");
            default -> serviceBuilder.withType("ClusterIP");
        }
        var serviceTpl = serviceBuilder
                .withPorts(
                        portsBuilder.build()
                ).endSpec().build();
        var serviceIns = client.services().inNamespace(nameSpace).withName(deploymentName + "-service");
        Service service = serviceIns.get();
        if (service != null) {
            if (update) {
                serviceIns.replace(serviceTpl);
            }
        } else {
            serviceIns.create(serviceTpl);
        }
        // 更新服务连接数据
        updateEndpoint(info, appInfo);
        // 重新发布应用(依赖 ingresses 时才需要,gsc自动同步路由)
        // AppsBind.build().publisher(appInfo);
        return true;
    }

    public boolean delete(String appId, String name) {
        return delete(appId, name, null);
    }

    public boolean delete(String appId, String deploymentName, JSONObject appInfo) {
        if (appInfo == null) {
            appInfo = AppsBind.getInfo(appId);
        }
        if (JSONObject.isInvalided(appInfo)) {
            return false;
        }
        int clusterId = appInfo.getInt("k8s");
        if (clusterId == 0) {
            return true;
        }
        getClient(clusterId);
        if (client == null) {
            return false;
        }
        String nameSpace = appInfo.getString("name");
        // 找到所有符合条件的 deployment 删除
        var deploymentIns = client.apps().deployments().inNamespace(nameSpace).withName(deploymentName + "-deployment");
        Deployment deployment;
        try {
            deployment = deploymentIns.get();
        } catch (Exception e) {
            return true;
        }
        if (deployment != null) {
            return false;
        }
        deploymentIns.delete();

        // 找到所有符合条件的 service 删除
        var serviceIns = client.services().inNamespace(nameSpace).withName(deploymentName + "-service");
        var service = serviceIns.get();
        if (service != null) {
            serviceIns.delete();
        }
        return true;
    }

    public boolean delete(int id) {
        JSONObject info = getInfo(id);
        if (JSONObject.isInvalided(info)) {
            return false;
        }
        return delete(info.getString("appid"), info.getString("name"));
        // 重新部署应用
        /*
        if (r) {
            AppsBind.build().publisher(info.getString("appid"));
        }
        return r;
        */
    }

    public boolean update(int id, JSONObject newInfo) {
        return update(id, newInfo, null, null);
    }

    public boolean update(int id, JSONObject newInfo, JSONObject appInfo, JSONObject serviceInfo) {
        JSONObject info = getInfo(id);
        if (JSONObject.isInvalided(info)) {
            return false;
        }
        // 在新环境下重新部署
        String currentAppId = info.getString("appid");
        String name = info.getString("name");
        info.put(newInfo);
        if (create(info, true, appInfo, serviceInfo)) {
            // 删除旧环境(新的名称不等于旧的部署名称,删除旧的部署)
            if (!info.getString("name").equals(name)) {
                return delete(currentAppId, name);
            }
        }
        // 重新部署应用
        // AppsBind.build().publisher(info.getString("appid"));
        return true;
    }

    // 更新服务镜像
    public boolean upgrade(int id, String dockerImage, String version) {
        JSONObject info = getInfo(id);
        if (JSONObject.isInvalided(info)) {
            return false;
        }
        JSONObject appInfo = AppsBind.getInfo(info.getString("appid"));
        if (JSONObject.isInvalided(appInfo)) {
            return false;
        }
        JSONObject clusterInfo = getClient(appInfo.getInt("k8s"));
        if (JSONObject.isInvalided(clusterInfo)) {
            return false;
        }
        if (client == null) {
            return false;
        }
        String nameSpace = appInfo.getString("name");
        String deployName = info.getString("name");
        var deploymentIns = client.apps().deployments().inNamespace(nameSpace).withName(deployName + "deployment");
        if (deploymentIns.get() == null) {
            return false;
        }
        String image = dockerImage + ":" + version;
        var deployment = deploymentIns.rolling().withTimeout(60, TimeUnit.MINUTES).updateImage(image);
        int scaleNo = deployment.getStatus().getReplicas();
        while (scaleNo < deployment.getStatus().getUpdatedReplicas()) {
            ThreadHelper.sleep(100);
        }
        return true;
    }

    // 获得部署所有pod和对应的状态
    public JSONObject getPodsStatus(int id) {
        JSONObject result = JSONObject.build();
        JSONObject info = getInfo(id);
        if (JSONObject.isInvalided(info)) {
            return null;
        }
        JSONObject appInfo = AppsBind.getInfo(info.getString("appid"));
        if (JSONObject.isInvalided(appInfo)) {
            return null;
        }
        JSONObject clusterInfo = getClient(appInfo.getInt("k8s"));
        if (JSONObject.isInvalided(clusterInfo)) {
            return null;
        }
        if (client == null) {
            return null;
        }
        String nameSpace = appInfo.getString("name");
        String deployName = info.getString("name");
        var arr = client.pods().inNamespace(nameSpace).withLabel("service", deployName).list().getItems();
        for (var pod : arr) {
            var status = pod.getStatus();
            var con_arr = status.getContainerStatuses();
            for (var con : con_arr) {
                JSONObject containerInfo = JSONObject.build();
                containerInfo.put("restart_count", con.getRestartCount())
                        .put("docker_name", con.getImage())
                        .put("status", con.getState().toString());
                String containerName = con.getName();
                result.put(containerName, containerInfo);
            }
        }
        return result;
    }
}
