package main.java.Api;

import common.java.Rpc.FilterReturn;
import common.java.Rpc.RpcBefore;

public class SdkManagerBefore extends RpcBefore {
    public SdkManagerBefore(){
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
}
