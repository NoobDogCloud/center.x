package main.java.Api;

import common.java.GscCommon.CheckModel;
import common.java.Rpc.FilterReturn;
import common.java.Rpc.RpcBefore;
import common.java.Rpc.RpcJsonFilterHelper;
import model.ClusterModel;

public class ClusterBefore extends RpcBefore {
    public ClusterBefore() {
        input((data, ids) -> {
            boolean isUpdate = ids != null;
            // 正常检测 json 输入
            FilterReturn r = RpcJsonFilterHelper.build(data, isUpdate)
                    .filter("registry", (info, name) ->
                            ClusterModel.check_k8s_registry(info.getJson(name)) ?
                                    FilterReturn.success() :
                                    FilterReturn.fail("私库信息无效")
                    )
                    .check()
                    .getResult();
            if (!r.isSuccess()) {
                return r;
            }
            if (isUpdate) {
                if (ids.length != 1) {
                    return FilterReturn.fail("只能更新一条数据");
                }
            }
            data.put("state", CheckModel.active);
            return FilterReturn.success();
        }).delete((ids) -> {
            // 判断是否有应用正在使用当前集群
            var appDb = (new Apps()).getDb();
            for (String id : ids) {
                if (appDb.eq("k8s", id).count() > 0) {
                    FilterReturn.fail("当前集群[" + id + "]正在使用，无法删除");
                }
            }
            return FilterReturn.success();
        });
    }
}
