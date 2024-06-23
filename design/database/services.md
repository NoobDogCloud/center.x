##### Microservice Configuration Model
| 字段名           | 数据类型   | 校验类型 | 初始值     | 错误值            | 约束                          | 关联                   | 标签         | 说明                           |
|---------------|--------|------|---------|----------------|-----------------------------|----------------------|------------|------------------------------|
| dockerimage   | string | 21   | ""      | ""             | Docker image URL format     | null                 | Docker镜像地址 | Docker镜像的完整地址，用于部署服务         |
| debug         | int    | 6    | 0       | -1             | [0, 1]                      | null                 | 调试模式       | 启用调试标志，0为关闭，1为开启             |
| datamodel     | string | 31   | "{}"    | "Invalid JSON" | Valid JSON schema           | User model           | 数据模型       | 定义了用户相关的数据模型和权限等复杂配置的JSON字符串 |
| dev_container | string | 21   | ""      | ""             | Docker container naming     | null                 | 开发容器       | 开发环境下的容器名称或路径                |
| kind          | string | 15   | ""      | ""             | ["user", "payment", ...]    | null                 | 服务类型       | 服务的类别标识                      |
| dev_ssh       | string | 11   | ""      | ""             | SSH连接格式                     | null                 | 开发SSH      | 连接开发环境的SSH信息                 |
| api           | string | 21   | ""      | ""             | API endpoint pattern        | null                 | API地址      | 服务API的访问地址                   |
| mq_config     | string | 21   | ""      | ""             | Message queue config format | null                 | 消息队列配置     | 消息队列的配置信息                    |
| userid        | string | 16   | ""      | ""             | UserID format               | null                 | 用户ID       | 创建或管理此配置的用户ID                |
| version       | string | 31   | "1.0.0" | "abc"          | SemVer format               | null                 | 服务版本       | 服务的当前版本号                     |
| sdk_id        | string | 31   | "0.0.0" | "abc"          | SemVer format               | null                 | SDK版本      | 关联SDK的版本号                    |
| protocol      | string | 15   | ""      | ""             | ["TCP", "UDP", ...]         | null                 | 通信协议       | 服务使用的通信协议                    |
| transfer      | string | 15   | ""      | ""             | ["http", "https", "grpc"]   | null                 | 数据传输方式     | 数据交互的传输协议                    |
| dev           | object | 31   | {}      | null           | JSON object                 | Development settings | 开发设置       | 开发阶段特定的配置信息                  |
| port          | int    | 6    | 0       | -1             | [1024, 65535]               | null                 | 服务端口       | 服务监听的端口号                     |
| name          | string | 15   | ""      | ""             | Alphanumeric & symbols      | Service Name         | 服务名称       | 微服务的标识名称                     |
| id            | int    | 6    | 0       | -1             | Positive integers           | Unique ID            | 服务ID       | 服务的唯一标识符                     |
| category      | string | 15   | ""      | ""             | Service categories          | null                 | 服务类别       | 服务的分类，如经典服务                  |
| config        | string | 31   | "{}"    | "Invalid JSON" | Valid JSON                  | Additional Config    | 额外配置       | 附加的配置信息                      |
| desc          | string | 1    | ""      | ""             | Text describing service     | null                 | 服务描述       | 对服务功能的简短描述                   |