package main.java.Api;

import db.service.ApplicationTemplate;
import main.java.Main.InitMaster;
import org.json.gsc.JSONObject;

public class Cluster extends ApplicationTemplate {
    public Cluster() {
        super("k8s");
    }

    public boolean update(String uids, JSONObject info) {
        var idArr = uids.split(",");
        if (idArr.length == 0) {
            return false;
        }
        /*
        if (!ClusterBind.build().update(Integer.parseInt(idArr[0]), info)) {
            return false;
        }
        */
        var r = super.update(uids, info);
        if (r) {
            InitMaster.Init();
        }
        return r;
    }
}