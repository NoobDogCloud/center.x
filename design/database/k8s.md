##### 配置模型
| 字段名      | 数据类型   | 校验类型 | 初始值 | 错误值 | 约束   | 关联   | 标签         | 说明          |
|----------|--------|------|-----|-----|------|------|------------|-------------|
| registry | string | 21   | ""  | ""  | null | null | Docker仓库地址 | Harbor的认证信息 |
| nodeips  | string | 20   | ""  | ""  | null | null | 节点IP地址     | K8s集群节点IP   |
| name     | string | 1    | ""  | ""  | null | null | 集群名称       | K8s集群名称     |
| cert     | string | 6    | ""  | ""  | null | null | 账号认证级别     | 如果有认证级别的话   |
| id       | int    | 6    | 0   | 0   | null | null | ID         | 数据项的ID      |
| state    | int    | 6    | 0   | 0   | null | null | 状态         | 数据项的状态      |
| userid   | string | 26   | ""  | ""  | null | null | 用户ID       | 用户的自定义账号    |
| config   | string | 2    | ""  | ""  | null | null | 配置         | K8s配置信息     |
| desc     | string | 2    | ""  | ""  | null | null | 描述         | 数据项的描述      |