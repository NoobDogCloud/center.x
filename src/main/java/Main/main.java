package main.java.Main;

import common.java.JGrapeSystem.GscBooster;
import install.Installer;

// -k grapeSoft@ -p 805
public class main {
    public static void main(String[] args) {
        if (Installer.install()) {
            System.out.println("<初始化...安装成功>");
        }
        System.out.println("<同步集群服务状态>");
        InitMaster.Init();
        System.out.println("<中心服务器>");
        GscBooster.start(args);
    }
}
