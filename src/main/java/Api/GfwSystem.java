package main.java.Api;

import common.java.Database.DBLayer;
import common.java.Database.DBManager;
import common.java.File.FileText;
import common.java.Http.Server.Upload.UploadFile;
import common.java.Rpc.RpcMessage;
import db.DbConfig;

import java.io.File;

public class GfwSystem {
    public File exportBackup(String fileName) {
        DBLayer db = DBLayer.buildWithConfig(DbConfig.getConfig().toString());
        File file = new File(FileText.buildTempFile("_backup.json"));
        DBManager.getInstance(db).doExport(file);
        return file;
    }

    public Object importBackup() {
        var files = UploadFile.getAll();
        if (files.size() != 1) {
            return RpcMessage.Instant(false, "请上传一个备份文件");
        }
        var file = files.get(0);
        DBLayer db = DBLayer.buildWithConfig(DbConfig.getConfig().toString());
        DBManager.getInstance(db).doImport(file.getFileInfo().getLocalFile());
        return true;
    }
}
