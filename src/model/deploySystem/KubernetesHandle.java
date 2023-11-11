package model.deploySystem;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.util.HashMap;

public class KubernetesHandle {
    private static final HashMap<String, KubernetesClient> kubernetesClientHashMap = new HashMap<>();

    public static KubernetesClient getInstance(String kubeConfig) {
        if (!kubernetesClientHashMap.containsKey(kubeConfig)) {
            try {
                KubernetesClient client = new DefaultKubernetesClient(Config.fromKubeconfig(kubeConfig));
                kubernetesClientHashMap.put(kubeConfig, client);
            } catch (Exception e) {
                // e.printStackTrace();
                return null;
            }
        }
        return kubernetesClientHashMap.get(kubeConfig);
    }
}
