package model;

import common.java.Apps.MicroService.MicroServiceContext;
import common.java.Check.CheckHelper;
import common.java.Database.DBLayer;
import main.java.Api.SubServer;
import org.json.gsc.JSONObject;

import java.util.Set;

public class ServiceModel {
    public static JSONObject removePrivateInfo(JSONObject info) {
        info.remove("dockerimage");
        info.remove("desc");
        info.remove("protocol");
        info.remove("dev_ssh");
        info.remove("dev_container");
        return info;
    }

    public static boolean check_transfer(String transferName) {
        Set<String> transferArr = MicroServiceContext.TransferKey();
        return transferArr.contains(transferName);
    }

    // 检查服务节点
    public static boolean check_peerAddr(String peerAddr) {
        if (peerAddr != null || peerAddr.length() == 0) {
            return true;
        }
        String[] pAddr = peerAddr.split(",");
        for (String s : pAddr) {
            String[] info = s.split(":");
            if (info.length != 2) {
                return false;
            }
        }
        return true;
    }

    // 检查订阅服务中间件节点
    public static boolean check_subAddr(String subServerIds) {
        if (subServerIds != null || subServerIds.length() == 0) {
            return true;
        }
        String[] idArr = subServerIds.split(",");
        SubServer ss = new SubServer();
        DBLayer db = ss.getDb().or();
        for (String s : idArr) {
            db.eq(db.getGeneratedKeys(), s);
        }
        return db.count() == idArr.length;
    }

    public static boolean check_name(String name) {
        return CheckHelper.IsStrictID(name, 64) &&
                !name.equals("default") &&
                !name.equals("system") &&
                !name.startsWith("kube-");
    }

    public static boolean check_protocol(String protocol) {
        return protocol.equals("TCP") || protocol.equals("UDP");
    }
}
