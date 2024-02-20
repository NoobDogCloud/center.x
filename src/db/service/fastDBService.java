package db.service;

import common.java.Database.DBFilter;
import common.java.Database.DBLayer;
import common.java.Rpc.RpcPageInfo;
import common.java.String.StringHelper;
import db.DbConfig;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

import java.util.List;
import java.util.function.Function;

public class fastDBService {
	private DBLayer db;
	private String mainKey;
	private String pk;

	public fastDBService(String tableName) {
		init(tableName, null);
	}

	public fastDBService(String tableName, String _pk) {
		init(tableName, _pk);
	}

	private void init(String tableName, String _pk) {
		mainKey = null;
		db = DBLayer.buildWithConfig(DbConfig.getConfig().toString());
		db.form(tableName);
		pk = _pk == null ? db.getGeneratedKeys() : _pk;
	}

	public void setMainKey(String key) {
		mainKey = key;
	}

	private String getKey() {
		return mainKey == null ? pk : mainKey;
	}

	public String testCond() {
		return db.getCond().toString();
	}

	public DBLayer _getDB() {
		return db;
	}

	public String getPk() {
		return pk;
	}


	public JSONArray<JSONObject> select() {
		return db.select();
	}

	public JSONArray<JSONObject> selectEx(JSONArray conds) {
		return db.where(conds).select();
	}

	public RpcPageInfo page(int idx, int max) {
		return pageBy(idx, max, null);
	}

	public RpcPageInfo pageEx(int idx, int max, JSONArray<JSONObject> conds) {
		return pageBy(idx, max, conds);
	}

	public JSONObject find(Object val) {//find
		return find(getKey(), val);
	}

	public JSONObject find(String key, Object val) {//find
		JSONObject rj = null;
		if (db != null) {
			rj = db.eq(key, val).find();
		}
		return rj;
	}

	public boolean insert(JSONObject json) {
		JSONArray<JSONObject> array = null;
		if (json != null) {
			if (json.containsKey(pk) && StringHelper.isInvalided(json.getString(pk))) {
				json.remove(pk);
			}
			array = new JSONArray<>();
			array.add(json);
		}
		return insertAll(array);
	}

	/**
	 * @apiNote 新增成功后执行 task, task 执行失败则删除新增的数据
	 */
	public boolean insertOrRollback(JSONObject json, Function<Object, Boolean> task) {
		Object lastId = db.data(json).insertOnce();
		if (!task.apply(lastId) && lastId != null) {
			db.eq(getKey(), lastId).delete();
			return false;
		}
		return true;
	}

	public RpcPageInfo pageBy(int idx, int max, JSONArray<JSONObject> conds) {
		JSONArray<JSONObject> rj = null;
		long count = 0;
		if (db != null) {
			if (!JSONArray.isInvalided(conds)) {
				db.where(conds);
			}
			db.dirty();
			String pk = db.getGeneratedKeys();
			if( pk != null && !pk.isEmpty()){
				db.desc(pk);
			}
			rj = db.page(idx, max);
			// count = db.pageMax(max);
			count = db.count();
		}
		return RpcPageInfo.Instant(idx, max, count, rj);
	}

	public boolean insertAll(JSONArray<JSONObject> array) {
		List<Object> rj = null;
		if (array != null && array.size() > 0) {
			if (db != null) {
				for (JSONObject o : array) {
					/*
					if (o.has(pk)) {
						o.remove(pk);
					}
					*/
					db.data(o);
				}
				rj = db.insert();
			}
		}
		return (rj != null && !rj.isEmpty());
	}

	public boolean update(String ids, JSONObject data) {
		long rj = 0;
		if (data != null) {
			if (db != null) {
				DBFilter dbf = DBFilter.buildDbFilter();
				String[] idList = ids.split(",");
				if (idList.length > 0) {
					for (String s : idList) {
						dbf.or().eq(getKey(), s);
					}
					rj = db.groupCondition(dbf.buildEx()).data(data).updateAll();
				}
			}
		}
		return (rj > 0);
	}


	public boolean delete(String ids) {
		long rj = 0;
		DBFilter dbf = DBFilter.buildDbFilter();
		String[] idList = ids.split(",");
		if (idList.length > 0) {
			for (String s : idList) {
				dbf.or().eq(getKey(), s);
			}
			rj = db.groupCondition(dbf.buildEx()).deleteAll();
		}
		return (rj > 0);
	}
}
