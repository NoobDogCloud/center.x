package main.java.Api;

import common.java.Apps.MicroService.Config.ModelServiceConfig;
import common.java.File.FileText;
import common.java.Http.Mime;
import common.java.Rpc.RpcMessage;
import common.java.Rpc.RpcMime;
import common.java.String.StringHelper;
import common.java.Xml.XmlHelper;
import db.service.ApplicationTemplate;
import main.java.Module.ServicesBind;
import model.ConfigModel;
import model.GscPomModel;
import org.json.gsc.JSONObject;

import java.io.File;
import java.nio.charset.StandardCharsets;

public class Services extends ApplicationTemplate {
    public Services() {
        super("services");
    }

    public boolean update(String uids, JSONObject info) {
        var idArr = uids.split(",");
        if (idArr.length == 0) {
            return false;
        }
        if (!ServicesBind.build().update(Integer.parseInt(idArr[0]), info)) {
            return false;
        }
        return super.update(uids, info);
    }

    // 下载特定服务的pom文件
    public Object downloadPom(String id){
        JSONObject info = find(id);
        if( JSONObject.isInvalided(info) ){
            return RpcMessage.Instant(false, "服务不存在");
        }
        String sdkVersion = info.getString("sdk_id");
        if( sdkVersion.isEmpty() ){
            return RpcMessage.Instant(false, "服务未指定SDK");
        }
        String serviceName = info.getString("name");
        if( serviceName.isEmpty() ){
            return RpcMessage.Instant(false, "服务未指定名称");
        }
        String versionString = info.getString("version");
        if( versionString.isEmpty() ){
            versionString = "0.0.0";
        }

        SdkManager sdk = new SdkManager();
        JSONObject sdkInfo = sdk.find(sdkVersion);
        if( JSONObject.isInvalided(sdkInfo) ){
            return RpcMessage.Instant(false, "SDK不存在");
        }
        var doc = XmlHelper.string2xml(sdkInfo.getString("text"));
        if( doc == null ){
            return RpcMessage.Instant(false, "SDK配置错误");
        }
        // 开始构造pom文件
        var gscPomModel = new GscPomModel(sdkVersion);
        gscPomModel.setServiceName(serviceName)
                .setVersion(versionString);
        JSONObject configInfo = info.getJson("config");
        if(!JSONObject.isInvalided(configInfo)){
            var configArr = ConfigModel.getConfigNameArr(configInfo);
            for( String configName : configArr ){
                gscPomModel.addDependency(configName);
            }
        }
        // return RpcMime.build("application/xml", StandardCharsets.UTF_8.toString(), gscPomModel.toString().getBytes(StandardCharsets.UTF_8));
        return gscPomModel.toString();
    }
}
