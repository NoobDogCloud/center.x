package main.java.Api;

import common.java.Rpc.FilterReturn;
import common.java.Rpc.RpcBefore;
import common.java.Rpc.RpcJsonFilterHelper;
import model.ServiceModel;

public class ServicesBefore extends RpcBefore {
    public ServicesBefore() {
        input((service, ids) -> {
            Services api = new Services();
            String pk = api.getDb().getGeneratedKeys();
            boolean isUpdate = ids != null;
            FilterReturn r = RpcJsonFilterHelper.build(service, ids != null)
                    .filter("transfer", (info, name) -> ServiceModel.check_transfer(info.getString(name)) ? FilterReturn.success() : FilterReturn.fail("无效服务通讯协议"),
                            true,
                            "服务通讯协议未定义")
                    .filter("subaddr", (info, name) -> ServiceModel.check_subAddr(info.getString(name)) ? FilterReturn.success() : FilterReturn.fail("无效订阅中间件"),
                            false)
                    .filter("name", (info, name) -> ServiceModel.check_name(info.getString(name)) ? FilterReturn.success() : FilterReturn.fail("无效服务名称"),
                            true)
                    .filter("protocol", (info, name) -> ServiceModel.check_protocol(info.getString(name)) ? FilterReturn.success() : FilterReturn.fail("无效网络协议"),
                            true)
                    .filterUnique("name", pk, (line) -> api.getDb().eq("name", line.getString("name")).limit(2).select())
                    .check().getResult();
            if (!r.isSuccess()) {
                return r;
            }
            if (isUpdate) {
                if (ids.length != 1) {
                    return FilterReturn.fail("只能更新一条数据");
                }
            }
            return FilterReturn.success();
        }).delete((ids) -> {
            // 判断应用是否包含部署
            var deployDb = (new ServicesDeploy()).getDb();
            for (var id : ids) {
                if (deployDb.eq("serviceid", id).count() > 0) {
                    return FilterReturn.fail("当前服务[" + id + "]正在使用，无法删除");
                }
            }
            return FilterReturn.success();
        });
    }
}
