package install;

import common.java.Database.DBLayer;
import common.java.Database.DBManager;
import common.java.Setup.ISetup;
import common.java.Setup.SetupBuilder;
import db.DbConfig;

import java.io.File;
import java.util.function.BooleanSupplier;

public class Installer implements ISetup {
    private static final BooleanSupplier func = () -> {
        DBLayer db = DBLayer.buildWithConfig(DbConfig.getConfig().toString());
        DBManager.getInstance(db).doImport(new File("./install.json"));
        return true;
    };

    public static boolean install() {
        return SetupBuilder.getInstance(func).install();
    }

    public static boolean reInstall() {
        return SetupBuilder.getInstance(func).reInstall();
    }
}
