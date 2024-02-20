package db.service;

import common.java.Database.DBLayer;
import common.java.InterfaceModel.Type.InterfaceType;
import common.java.Rpc.RpcPageInfo;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

import java.util.function.Function;

public class BaseTemplate {
    protected fastDBService fdb;

    public BaseTemplate(String tableName) {
        init(tableName);
    }

    @InterfaceType(InterfaceType.type.CloseApi)
    public void init(String tableName) {
        fdb = new fastDBService(tableName, null);
    }

    @InterfaceType(InterfaceType.type.CloseApi)
    public DBLayer getDb() {
        return fdb._getDB();
    }

    public JSONArray select() {
        return fdb.select();
    }

    public JSONArray selectEx(JSONArray cond) {
        return fdb.selectEx(cond);
    }

    public RpcPageInfo page(int idx, int max) {
        return fdb.page(idx, max);
    }

    public RpcPageInfo pageEx(int idx, int max, JSONArray cond) {
        return fdb.pageEx(idx, max, cond);
    }

    public boolean update(String uids, JSONObject info) {
        return fdb.update(uids, info);
    }

    public boolean delete(String uids) {
        return fdb.delete(uids);
    }

    public Object insert(JSONObject nObj) {
        return fdb.insert(nObj);
    }

    protected boolean insertOrRollback(JSONObject nObj, Function<Object, Boolean> func) {
        return fdb.insertOrRollback(nObj, func);
    }

    public JSONObject find(Object id) {
        return fdb.find(id);
    }

    public JSONObject find(String key, Object id) {
        return fdb.find(key, id);
    }
}
