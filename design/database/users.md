##### User Profile Model
| 字段名      | 数据类型   | 校验类型 | 初始值 | 错误值 | 约束                     | 关联       | 标签   | 说明              |
|----------|--------|------|-----|-----|------------------------|----------|------|-----------------|
| password | string | 22   | ""  | ""  | Hashed & Salted        | null     | 密码   | 用户账户密码，存储为哈希加盐值 |
| salt     | string | 26   | ""  | ""  | Alphanumeric           | password | 密码盐  | 用于增强密码安全性的随机字符串 |
| phone    | string | 11   | ""  | ""  | E.164 format           | null     | 手机号码 | 用户绑定的手机号码       |
| level    | int    | 6    | 0   | -1  | [0, ∞)                 | null     | 权限等级 | 用户在系统中的权限层级     |
| name     | string | 15   | ""  | ""  | Alphanumeric & Chinese | null     | 姓名   | 用户的真实姓名或昵称      |
| avatar   | string | 21   | ""  | ""  | URL format             | null     | 头像链接 | 用户头像的网络地址       |
| title    | string | 1    | ""  | ""  | Alphanumeric & symbols | null     | 用户头衔 | 用户的角色或职位名称      |
| userid   | string | 26   | ""  | ""  | UserID format          | null     | 用户ID | 系统内用户账号的唯一标识符   |