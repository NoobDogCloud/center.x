package model;

import main.java.Api.SdkManager;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.json.gsc.JSONObject;

import java.util.HashSet;
import java.util.Set;

public class GscPomModel {
    /*
    private static final HashMap<String, String> DependencieMap = new HashMap<>() {{
        put("mongodb", "org.mongodb:mongodb-driver-sync");
        put("mysql", "com.mysql:mysql-connector-j");
        put("h2", "com.h2database:h2");
        put("oracle", "com.oracle.database.jdbc:ojdbc10");
        put("redis", "io.lettuce:lettuce-core");
        put("pulsar", "org.apache.pulsar:pulsar-client");
        put("rabbitmq", "com.rabbitmq:amqp-client");
        put("kafka", "org.apache.kafka:kafka_2.13");
        put("activemq", "org.apache.activemq:activemq-client");
    }};
    */

    private Document GscPomDocumentTemplate;
    private final String sdkVersion;
    private final JSONObject dependenciesInfo;
    // private String appName;
    private String serviceName;
    private String version;
    private Set<String> dependencies = new HashSet<>();

    private JSONObject sdkInfo;
    public GscPomModel(String sdkVersion){
        try {
            GscPomDocumentTemplate = DocumentHelper.parseText("<?xml version=\"1.0\" encoding=\"UTF-8\"?><project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\"> <modelVersion>4.0.0</modelVersion> <groupId>io.github.noobdogcloud</groupId> <artifactId>SDK</artifactId> <version>0.0.3</version> <properties> <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding> </properties> <dependencies> </dependencies> <build> <sourceDirectory>${project.basedir}/src</sourceDirectory> <testSourceDirectory>${project.basedir}/test</testSourceDirectory> <plugins> <plugin> <groupId>org.apache.maven.plugins</groupId> <artifactId>maven-source-plugin</artifactId> <version>3.0.1</version> <executions> <execution> <id>attach-sources</id> <phase>verify</phase> <goals> <goal>jar-no-fork</goal> </goals> </execution> </executions> </plugin> <plugin> <groupId>org.apache.maven.plugins</groupId> <artifactId>maven-javadoc-plugin</artifactId> <version>3.4.0</version> <configuration> <charset>UTF-8</charset> <docencoding>UTF-8</docencoding> <encoding>UTF-8</encoding> </configuration> <executions> <execution> <id>attach-javadocs</id> <goals> <goal>jar</goal> </goals> </execution> </executions> </plugin> <plugin> <groupId>org.apache.maven.plugins</groupId> <artifactId>maven-compiler-plugin</artifactId> <version>3.8.1</version> <configuration> <source>21</source> <target>21</target> <encoding>UTF-8</encoding> </configuration> </plugin> <plugin> <groupId>org.apache.maven.plugins</groupId> <artifactId>maven-assembly-plugin</artifactId> <version>3.1.1</version> <configuration> <descriptorRefs> <descriptorRef>jar-with-dependencies</descriptorRef> </descriptorRefs> <archive> <manifest> <mainClass>main.java.Main.main</mainClass> </manifest> </archive> </configuration> <executions> <execution> <id>make-assembly</id> <phase>package</phase> <goals> <goal>single</goal> </goals> </execution> </executions> </plugin> <plugin> <groupId>org.apache.maven.plugins</groupId> <artifactId>maven-gpg-plugin</artifactId> <version>1.5</version> <executions> <execution> <id>sign-artifacts</id> <phase>verify</phase> <goals> <goal>sign</goal> </goals> </execution> </executions> </plugin> <plugin> <groupId>org.apache.maven.plugins</groupId> <artifactId>maven-release-plugin</artifactId> <version>2.5.3</version> <configuration> <autoVersionSubmodules>true</autoVersionSubmodules> <useReleaseProfile>false</useReleaseProfile> <releaseProfiles>release</releaseProfiles> <goals>deploy</goals> </configuration> </plugin> </plugins> </build> <profiles> <profile> <id>release-sign-artifacts</id> <activation> <property> <name>performRelease</name> <value>true</value> </property> </activation> </profile> </profiles> <distributionManagement> <repository> <id>gsc-releases</id> <url>http://nexus.putao282.com:8081/repository/maven-releases</url> </repository> <snapshotRepository> <id>gsc-snapshots</id> <url>http://nexus.putao282.com:8081/repository/maven-snapshots</url> </snapshotRepository> </distributionManagement></project>");
        } catch (DocumentException e) {
            throw new RuntimeException(e);
        }
        this.sdkVersion = sdkVersion;
        SdkManager sdkManager = new SdkManager();
        this.sdkInfo = sdkManager.getDb().eq("version", this.version).find();
        if( JSONObject.isInvalided(this.sdkInfo) ){
            throw new RuntimeException("SDK版本不存在");
        }
        this.dependenciesInfo = this.sdkInfo.getJson("dependencies_template");
        /**
         * {
         *     "mongodb": {
         *         "groupId": "org.mongodb",
         *         "artifactId": "mongodb-driver-sync",
         *         "version": "4.3.3",
         *     },
         * }
         * */
    }

    public GscPomModel addDependency(String middlewareName) {
        this.dependencies.add(middlewareName);
        return this;
    }

    private void addJunit(Element dependencies){
        Element dependency = dependencies.addElement("dependency");
        dependency.addElement("groupId").setText("junit");
        dependency.addElement("artifactId").setText("junit");
        dependency.addElement("version").setText("4.13.2");
        dependency.addElement("scope").setText("test");
    }

    private void addGscSDK(Element dependencies){
        Element dependency = dependencies.addElement("dependency");
        dependency.addElement("groupId").setText("io.github.noobdogcloud");
        dependency.addElement("artifactId").setText("SDK");
        dependency.addElement("version").setText(this.sdkVersion);
    }

    private void buildDependencies(){
        Element root = GscPomDocumentTemplate.getRootElement();
        Element dependencies = root.element("dependencies");
        // 增加 junit
        this.addJunit(dependencies);
        // 增加 sdk
        this.addGscSDK(dependencies);

        for(String middlewareName : this.dependencies){
            JSONObject middlewareInfo = this.dependenciesInfo.getJson(middlewareName);
            if( JSONObject.isInvalided(middlewareInfo) ){
                throw new RuntimeException("中间件不存在");
            }
            Element dependency = dependencies.addElement("dependency");
            dependency.addElement("groupId").setText(middlewareInfo.getString("groupId"));
            dependency.addElement("artifactId").setText(middlewareInfo.getString("artifactId"));
            dependency.addElement("version").setText(middlewareInfo.getString("version"));
        }
    }

    private void updateMeta(){
        Element root = GscPomDocumentTemplate.getRootElement();
        root.element("version").setText(this.version);
        root.element("artifactId").setText(this.serviceName);
        // root.element("groupId").setText(this.appName);
    }

    /*
    public GscPomModel setAppName(String appName) {
        this.appName = appName;
        return this;
    }
    */

    public GscPomModel setServiceName(String serviceName) {
        this.serviceName = serviceName;
        return this;
    }

    public GscPomModel setVersion(String version) {
        this.version = version;
        return this;
    }

    public String toString() {
        this.buildDependencies();
        this.updateMeta();
        return this.GscPomDocumentTemplate.asXML();
    }
}
