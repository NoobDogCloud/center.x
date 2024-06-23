##### 服务配置模型
| 字段名                 | 数据类型      | 校验类型 | 初始值  | 错误值  | 约束   | 关联   | 标签         | 说明                                            |
|---------------------|-----------|------|------|------|------|------|------------|-----------------------------------------------|
| logs_service_id     | int       | 6    | 0    | 0    | null | null | 日志服务ID     |                                               |
| workflow_service_id | int       | 6    | 0    | 0    | null | null | 工作流服务ID    |                                               |
| roles               | string    | 2    | "{}" | "{}" | null | null | 角色         |                                               |
| k8s                 | int       | 6    | 0    | 0    | null | null | Kubernetes |                                               |
| updateat            | timestamp | 27   | 0    | 0    | null | null | 更新时间       | 2022-07-22 06:17:43.0                         |
| secret              | string    | 22   | ""   | ""   | null | null | 密码         | grapeSoft@                                    |
| userid              | string    | 26   | ""   | ""   | null | null | 用户ID       | putao520                                      |
| createat            | timestamp | 27   | 0    | 0    | null | null | 创建时间       | 2022-02-24 18:52:08.0                         |
| master              | string    | 20   | ""   | ""   | null | null | 主节点        | 12.12.12.120:805                              |
| entry               | string    | 21   | ""   | ""   | null | null | 入口         | http://localhost:9000                         |
| domain              | string    | 21   | ""   | ""   | null | null | 域名         | ingress.test.local                            |
| name                | string    | 26   | ""   | ""   | null | null | 服务名称       | test-demo                                     |
| id                  | string    | 26   | ""   | ""   | null | null | ID         | 1                                             |
| session_type        | string    | 26   | ""   | ""   | null | null | 会话类型       | jwt                                           |
| state               | int       | 6    | 0    | 0    | null | null | 状态         | 1                                             |
| category            | string    | 26   | ""   | ""   | null | null | 分类         | secgateway-service                            |
| config              | string    | 2    | ""   | ""   | null | null | 配置         | {"db":"mongodb","cache":"redis","other":"[]"} |
| dev_secret          | string    | 22   | ""   | ""   | null | null | 开发环境密码     | grapeSoft@                                    |
| user_service_id     | int       | 6    | 1    | 1    | null | null | 用户服务ID     |                                               |
| desc                | string    | 2    | ""   | ""   | null | null | 描述         | 用于开发测试环境                                      |