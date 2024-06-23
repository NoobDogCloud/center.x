package db.service;

import common.java.Database.DBLayer;
import common.java.InterfaceModel.Type.InterfaceType;
import common.java.Rpc.RpcPageInfo;
import common.java.Session.UserSession;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

import java.util.function.Function;

public class ApplicationTemplate extends BaseTemplate{
    private final String userId;

    public ApplicationTemplate(String tableName) {
        super(tableName);
        UserSession session = UserSession.current();
        if (!session.checkSession()) {
            throw new RuntimeException("用户未登录");
        }
        this.userId = session.getUID();
        super.fdb._getDB().addConstantCond("userid", this.userId);
    }

    public boolean update(String uids, JSONObject info) {
        if( info.has("userid") ){
            info.remove("userid");
        }
        return fdb.update(uids, info);
    }

    public Object insert(JSONObject nObj) {
        // 强制添加 userid
        nObj.put("userid", this.userId);
        return fdb.insert(nObj);
    }

    protected boolean insertOrRollback(JSONObject nObj, Function<Object, Boolean> func) {
        // 强制添加 userid
        nObj.put("userid", this.userId);
        return fdb.insertOrRollback(nObj, func);
    }
}
