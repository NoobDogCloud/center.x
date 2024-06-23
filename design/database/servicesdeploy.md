##### Service Configuration Model
| 字段名          | 数据类型   | 校验类型 | 初始值     | 错误值            | 约束                     | 关联                     | 标签     | 说明                                |
|--------------|--------|------|---------|----------------|------------------------|------------------------|--------|-----------------------------------|
| container    | int    | 6    | 0       | -1             | [1, ∞)                 | null                   | 容器实例数  | 运行容器的数量                           |
| debug        | int    | 6    | 0       | -1             | [0, 1]                 | null                   | 调试模式   | 开启或关闭调试标志                         |
| datamodel    | string | 31   | "{}"    | "Invalid JSON" | Valid JSON schema      | Complex Data Structure | 数据模型定义 | 包含多个实体的数据权限、API接口及字段验证规则的复杂JSON对象 |
| updateat     | string | 18   | ""      | "Invalid date" | ISO 8601 datetime      | null                   | 更新时间   | 最近更新的时间戳                          |
| secure       | int    | 6    | 0       | -1             | [0, 1]                 | null                   | 安全模式   | 安全性配置状态                           |
| replicaset   | int    | 6    | 0       | -1             | [1, ∞)                 | null                   | 副本集数量  | 数据库副本集的实例数量                       |
| userid       | string | 16   | ""      | ""             | UserID format          | null                   | 用户ID   | 操作或创建该配置的用户标识                     |
| version      | string | 31   | "1.0.0" | "abc"          | SemVer format          | null                   | 版本号    | 服务或配置的版本标识                        |
| createat     | string | 18   | ""      | "Invalid date" | ISO 8601 datetime      | null                   | 创建时间   | 配置创建的时间戳                          |
| clusteraddr  | string | 21   | ""      | ""             | IP:port format         | null                   | 集群地址   | 服务集群的地址和端口                        |
| target_port  | int    | 6    | 0       | -1             | [0, 65535]             | null                   | 目标端口   | 服务期望映射的端口                         |
| appid        | string | 31   | "0"     | "-1"           | App Identifier format  | null                   | 应用ID   | 关联的应用程序标识                         |
| subaddr      | string | 21   | ""      | ""             | IP:port format         | null                   | 子服务地址  | 子服务的具体地址和端口                       |
| name         | string | 15   | ""      | ""             | Alphanumeric & symbols | Service/Dataset Name   | 名称     | 服务或数据集的名称                         |
| proxy_target | array  | 31   | "[]"    | "Invalid JSON" | JSON array             | Proxy Targets          | 代理目标列表 | 代理服务的目标地址列表                       |
| id           | int    | 6    | 0       | -1             | Positive integers      | Unique Identifier      | 唯一ID   | 配置项的唯一标识                          |
| text         | string | 1    | ""      | ""             | Text description       | null                   | 描述     | 服务或配置的简短描述                        |
| state        | int    | 6    | 0       | -1             | Service states         | null                   | 状态     | 服务运行状态标识                          |
| serviceid    | int    | 6    | 0       | -1             | Positive integers      | Service Identifier     | 服务ID   | 关联服务的标识                           |
| config       | string | 31   | "{}"    | "Invalid JSON" | Valid JSON             | Service Configurations | 配置详情   | 服务的详细配置信息，如缓存、消息队列等               |
| open         | int    | 6    | 0       | -1             | [0, 1]                 | null                   | 开关状态   | 服务对外开放的开关                         |