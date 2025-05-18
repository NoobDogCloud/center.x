package model;

public enum MiddlewareType {
    DB,      // 数据库
    CACHE,   // 缓存
    MQ;      // 消息队列

    public static MiddlewareType fromString(String type) {
        switch (type.toLowerCase()) {
            case "db":
                return DB;
            case "cache":
                return CACHE;
            case "mq":
                return MQ;
            default:
                throw new IllegalArgumentException("Unknown type: " + type);
        }
    }
}
