package main.java.Api;

import db.service.ApplicationTemplate;
import main.java.Module.ServicesBind;
import org.json.gsc.JSONObject;

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
}
