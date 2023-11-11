package model.deploySystem;

import common.java.Encrypt.Base64;
import org.json.gsc.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class KubeSecretDockerConfigJson {
    private final String host;
    private String username;
    private String password;
    private String email;
    private String auth;

    private KubeSecretDockerConfigJson(String host) {
        this.host = host;
        this.auth = null;
        this.email = "";
        this.password = null;
        this.username = null;
    }

    public static String getType() {
        return "kubernetes.io/dockerconfigjson";
    }

    public static KubeSecretDockerConfigJson build(String host) {
        return new KubeSecretDockerConfigJson(host);
    }

    public static KubeSecretDockerConfigJson build(JSONObject gsc_registry) {
        int port = gsc_registry.getInt("port");
        String registry = gsc_registry.getString("domain") +
                (port == 80 ? "" : ":" + gsc_registry.getInt("port"));
        String username = gsc_registry.getString("id");
        String password = gsc_registry.getString("password");
        return KubeSecretDockerConfigJson.build(registry)
                .setUsername(username)
                .setPassword(password);
    }

    public String getUsername() {
        return username;
    }

    public KubeSecretDockerConfigJson setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public KubeSecretDockerConfigJson setPassword(String password) {
        this.password = password;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public KubeSecretDockerConfigJson setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getAuth() {
        return auth;
    }

    public KubeSecretDockerConfigJson setAuth(String auth) {
        this.auth = auth;
        return this;
    }

    public Map<String, String> build() {
        JSONObject data = new JSONObject();
        data.put("username", username);
        data.put("password", password);
        data.put("email", email);
        if (auth != null) {
            data.put("auth", auth);
        }
        JSONObject auths = new JSONObject();
        auths.put("auths", JSONObject.build().put(host, data));
        var dockerConfigJson = Base64.encode(auths.toString());
        Map<String, String> dockerConfig = new HashMap<>();
        dockerConfig.put(".dockerconfigjson", dockerConfigJson);
        return dockerConfig;
    }
}
