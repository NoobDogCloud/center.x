package db;

import org.json.gsc.JSONObject;

public class DbConfig {
    public static JSONObject getConfig() {
        return JSONObject.build().put("dbName", "h2")
                .put("user", "sa")
                .put("password", "")
                .put("database", "gsc")
                .put("initsize", 2)
                .put("maxactive", 10)
                .put("minidle", 2)
                .put("maxwait", 5000)
                .put("validationquery", "VALUES 1")
                .put("class", "org.h2.Driver")
                // .put("class", "org.h2.jdbcx.JdbcDataSource")
                .put("spilt", ";")
                .put("start", ";")
                .put("database_to_lower", true)
                .put("case_insensitive_identifiers", true)
                .put("mode", "MySQL")
                // .put("host", "h2:file:db/data");
                .put("host", "h2:file:./db/data");
    }

}
