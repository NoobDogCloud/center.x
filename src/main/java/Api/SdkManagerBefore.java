package main.java.Api;

import common.java.Encrypt.Base64;
import common.java.Rpc.FilterReturn;
import common.java.Rpc.RpcBefore;
import model.GscPomModel;
import model.MiddlewareName;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.json.gsc.JSONObject;

public class SdkManagerBefore extends RpcBefore {
    public SdkManagerBefore(){
        input((body, ids) -> {
            // boolean isUpdate = ids != null;
            if( body.has("dependencies_template") ){
                body.remove(("dependencies_template"));
            }
            if( body.has("text")) {
                // String pureText = Base64.decode(body.getString("text"));
                String pureText = body.getString("text");
                if(pureText.isEmpty()){
                    return FilterReturn.fail("无效的sdk文本");
                }
                var dependenciesTemplate = buildDependencyTemplate(pureText);
                if( dependenciesTemplate == null ){
                    return FilterReturn.fail("无效的sdk文本");
                }
                body.put("dependencies_template", dependenciesTemplate);
                body.put("text", pureText);
            }
            return FilterReturn.success();
        });

        delete((ids)->{
            // 判断服务是否引用了该sdk
            var services = new Services();
            var used_service = services.getDb().in("sdk_id", ids).count();
            // 判断配置是否引用了该sdk
            var configs = new Configs();
            var used_config = configs.getDb().in("sdk_id", ids).count();
            // 判断配置模板是否引用了该sdk
            var config_templates = new ConfigTemplates();
            var used_config_template = config_templates.getDb().in("sdk_id", ids).count();
            if(used_service > 0 || used_config > 0 || used_config_template > 0){
                return FilterReturn.fail("该sdk已被服务[" + used_service + "]、配置[" + used_config + "]、配置模板引用[" + used_config_template + "]，无法删除");
            }
            return FilterReturn.success();
        });
    }

    public static JSONObject buildDependencyTemplate(String pureText) {
        try {
            Document doc = DocumentHelper.parseText(pureText);
            var root = doc.getRootElement();
            var dependencies = root.element("dependencies");
            if (dependencies == null) {
                return null;
            }
            JSONObject dependenciesTemplate = JSONObject.build();
            var mnArr = MiddlewareName.values();
            for (MiddlewareName mn : mnArr) {
                var depsPath = mn.getDeps();
                var version = GscPomModel.FindDependencyVersionByPath(dependencies, depsPath);
                if( version == null ) {
                    continue;
                }
                String[] groupInfo =depsPath.split("/");
                if( groupInfo.length != 2 ) {
                    continue;
                }
                dependenciesTemplate.put(mn.getName(),
                        JSONObject.build()
                                .put("groupId", groupInfo[0])
                                .put("artifactId", groupInfo[1])
                                .put("version", version)
                );
            }
            return dependenciesTemplate;
        } catch (DocumentException e) {
            throw new RuntimeException(e);
        }
    }
}
