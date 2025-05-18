package model;

public enum MiddlewareName {
    MONGODB("mongodb", "db", "org.mongodb/mongodb-driver-sync"),
    MYSQL("mysql", "db", "com.mysql/mysql-connector-j"),
    H2("h2", "db", "com.h2database/和"),
    ORACLE("oracle", "db", "com.oracle.database.jdbc/ojdbc10"),
    REDIS("redis", "cache", "io.lettuce/lettuce-core"),
    PULSAR("pulsar", "mq", "org.apache.pulsar/pulsar-client"),
    RABBITMQ("rabbitmq", "mq", "com.rabbitmq/amqp-client"),
    KAFKA("kafka", "mq", "org.apache.kafka/kafka_2.13"),
    ACTIVEMQ("activemq", "mq", "org.apache.activemq/activemq-client");

    private final String name;
    private final String type;
    private final String deps;

    MiddlewareName(String name, String type, String deps) {
        this.name = name;
        this.type = type;
        this.deps = deps;
    }

    public static String[] getAllName(){
        String[] allName = new String[MiddlewareName.values().length];
        for (int i = 0; i < MiddlewareName.values().length; i++) {
            allName[i] = MiddlewareName.values()[i].name;
        }
        return allName;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getDeps() {
        return deps;
    }

    public MiddlewareType getMiddlewareType() {
        return MiddlewareType.fromString(type);
    }

    // 关键：通过 name 获得 MiddlewareType
    public static MiddlewareType getTypeByName(String name) {
        for (MiddlewareName middleware : MiddlewareName.values()) {
            if (middleware.name.equalsIgnoreCase(name)) {
                return middleware.getMiddlewareType();
            }
        }
        throw new IllegalArgumentException("Unknown middleware name: " + name);
    }
}
