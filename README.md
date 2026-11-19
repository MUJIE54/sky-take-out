# 苍穹外卖（Sky Take Out）

基于 Spring Boot 的外卖点餐与商家管理后端，配套管理端前端页面和数据库脚本，克隆后按本文档配置即可在本机跑通。

## 技术栈

- Spring Boot 2.7.3 / Spring MVC / MyBatis / PageHelper
- MySQL 8 + Druid 连接池
- Redis（店铺状态、菜品缓存）
- JWT 登录鉴权 + ThreadLocal 保存当前登录用户
- AOP 自动填充公共字段（创建人/更新时间等）
- WebSocket（来单提醒、客户催单）
- Apache POI（运营数据报表导出）
- 阿里云 OSS（图片上传）
- 微信小程序登录 / 微信支付（可选，不配置不影响管理端）

## 目录结构

```
sky-take-out
├── sky-common/          # 公共模块：常量、工具类、自定义注解、异常
├── sky-pojo/            # 实体、DTO、VO
├── sky-server/          # 服务端：controller / service / mapper / 配置
│   └── src/main/resources
│       ├── application.yml
│       ├── application-dev.yml   # 本机配置（数据库、Redis、OSS、微信）
│       ├── mapper/*.xml
│       └── template/运营数据报表模板.xlsx
├── sql/sky.sql          # 建库建表 + 初始化数据
├── nginx/
│   ├── nginx.conf       # 管理端前端 + 反向代理配置
│   └── html/sky/        # 管理端前端静态页面
├── miniprogram/         # 微信小程序源码（用户点餐端）
└── README.md
```

## 环境要求

| 软件 | 版本 | 说明 |
| --- | --- | --- |
| JDK | **17** | 不要用 21/26，项目里的 Lombok 1.18.20 在新 JDK 上会报 `ExceptionInInitializerError ... TypeTag :: UNKNOWN` |
| Maven | 3.6+ | 也可以用 IDEA 自带的 Maven |
| MySQL | 8.x | 端口 3306 |
| Redis | 5.x+ | 端口 6379 |
| Nginx | 1.20.2（Windows 版） | 用于托管管理端页面，端口 80 |

## 快速开始

### 1. 克隆项目

```bash
git clone https://github.com/MUJIE54/sky-take-out.git
cd sky-take-out
```

### 2. 导入数据库

`sql/sky.sql` 中已经包含 `CREATE DATABASE`，直接执行即可：

```bash
mysql -u root -p < sql/sky.sql
```

执行完成后应存在数据库 `sky_take_out`，其中已初始化菜品分类、菜品、套餐、员工账号等数据。

### 3. 修改本机配置

编辑 `sky-server/src/main/resources/application-dev.yml`，把下面几项改成你本机的值：

```yaml
sky:
  datasource:
    host: localhost
    port: 3306
    database: sky_take_out
    username: root
    password: root          # 改成你的 MySQL 密码
  redis:
    host: localhost
    port: 6379
    password: "123456"      # 你的 Redis 密码；没有密码就改成 ""
    database: 10
  alioss:
    access-key-id: <YOUR_ALIYUN_ACCESS_KEY_ID>        # 不上传图片可以先不填
    access-key-secret: <YOUR_ALIYUN_ACCESS_KEY_SECRET>
    bucket-name: sky-take-out
  wechat:
    appid: <YOUR_WECHAT_APPID>                        # 不用小程序端可以先不填
    secret: <YOUR_WECHAT_SECRET>
```

> 说明：仓库里提交的都是占位符。真实密钥建议写在 `sky-server/src/main/resources/application-local.yml`，该文件已被 `.gitignore` 忽略；`application.yml` 默认激活了 `dev,local` 两个 profile，后者的同项配置会覆盖前者，这样值不会误传到 GitHub。直接改 `application-dev.yml` 也能跑，但要注意别把真实值提交上去。

### 4. 启动后端

先确保 MySQL 和 Redis 已启动，然后在项目根目录执行：

```bash
mvn clean package -DskipTests
java -jar sky-server/target/sky-server-1.0-SNAPSHOT.jar
```

也可以直接用 IDEA 打开项目，运行 `sky-server` 模块下的 `com.sky.SkyApplication`。

启动成功后后端监听 `http://localhost:8080`，接口文档地址 `http://localhost:8080/doc.html`。

### 5. 启动管理端前端

1. 下载并解压 Nginx（Windows 版），例如解压到 `D:\nginx-1.20.2`。
2. 把仓库里的 `nginx/html/sky` 整个目录复制到 Nginx 安装目录的 `html` 下，得到 `D:\nginx-1.20.2\html\sky`。
3. 用仓库里的 `nginx/nginx.conf` 覆盖 `D:\nginx-1.20.2\conf\nginx.conf`。
4. 启动 Nginx：

   ```powershell
   cd D:\nginx-1.20.2
   .\nginx.exe
   ```

5. 浏览器访问 `http://localhost/`，默认管理员账号：

   - 用户名：`admin`
   - 密码：`123456`

## 端口与访问入口

| 服务 | 端口 | 地址 |
| --- | --- | --- |
| 管理端页面（Nginx） | 80 | http://localhost/ |
| 后端服务 | 8080 | http://localhost:8080 |
| 接口文档（Knife4j） | 8080 | http://localhost:8080/doc.html |
| MySQL | 3306 | - |
| Redis | 6379 | - |

Nginx 会把请求转发给后端：

- `/api/` → `http://localhost:8080/admin/`
- `/user/` → `http://localhost:8080/user/`
- `/ws/` → WebSocket 连接（来单提醒、催单）

## 可选：小程序端与第三方能力

- **微信小程序端（用户点餐）**：源码在仓库 `miniprogram/` 目录，用微信开发者工具「导入项目」选择该目录即可打开。
  - `project.config.json` 里的 `appid` 是课程示例值，换成你自己的小程序 appid；
  - 本地调试时在开发者工具「详情 → 本地设置」勾选“不校验合法域名、web-view（业务域名）、TLS 版本以及 HTTPS 证书”；
  - 请求地址在 `common/vendor.js` 中搜索 `baseUrl`，默认 `http://localhost:8080`；真机预览时改成电脑的局域网 IP，例如 `http://192.168.1.10:8080`，并保证手机和电脑在同一 WiFi；
  - 后端 `sky.wechat.appid` / `sky.wechat.secret` 要填成同一个小程序的值，否则微信登录会失败。
- **图片上传（阿里云 OSS）**：不配置 `sky.alioss` 时，新增菜品上传图片会失败，其余增删改查不受影响。
- **微信支付**：需要商户号、API 证书和可公网访问的 `notifyUrl`，本机学习环境一般用不到。

## 常见问题

**1. 启动报 `ExceptionInInitializerError: com.sun.tools.javac.code.TypeTag :: UNKNOWN`**

JDK 版本过高导致 Lombok 不兼容。把项目 SDK 和运行 JDK 都换成 **JDK 17**。

**2. 启动报 `Access denied for user 'root'@'localhost'`**

`application-dev.yml` 里的 `sky.datasource.password` 和本机 MySQL 密码不一致。

**3. 启动报 `NOAUTH Authentication required` / Redis 连接失败**

Redis 设置了密码但配置里的 `sky.redis.password` 没填，或者填了密码但 Redis 实际没有密码（此时应改为 `""`）。

**4. 打开 http://localhost/ 白屏或接口 404**

- 确认 `8080` 的后端已启动；
- 确认 Nginx 的 `html/sky` 目录存在且 `conf/nginx.conf` 已替换；
- 修改配置后需要重启 Nginx：`.\nginx.exe -s reload`。

**5. 8080 端口被占用**

改 `application.yml` 中的 `server.port`，同时同步修改 `nginx/nginx.conf` 里 `localhost:8080` 的转发地址。

**6. 导出运营数据报表报错**

确认 `sky-server/src/main/resources/template/运营数据报表模板.xlsx` 存在（仓库已包含）。

**7. 新增员工后无法登录**

当前登录逻辑是明文比对，而新增员工时密码存的是 MD5。练习阶段直接用初始化数据里的 `admin / 123456` 登录即可。
