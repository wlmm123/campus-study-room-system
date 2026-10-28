# 校园自习室分布式预约管理系统 · REST接口契约

- **版本**：V3.0（基线初稿，待评审冻结）
- **编制人**：成员2（系统设计负责人）
- **日期**：2026年10月28日

## 1. 全局约定

### 1.1 统一 JSON 返回格式

所有接口（含跨节点内部接口）必须返回以下统一结构：

```json
{
  "code": 200,          // 业务状态码
  "message": "操作成功", // 提示信息
  "data": {}            // 实际业务数据（无数据时为 {} 或 null）
}
```

### 1.2 全局 HTTP 状态码与业务错误码表

- `200`：成功

- `400`：仅用于请求参数缺失、类型错误、格式错误等参数问题（如时间格式错误、缺少必填项）

- `401`：Token 缺失、格式非法、签名无效或过期，统一返回 401（前端需拦截并跳转登录）

- `403`：无权限（如普通学生调用管理员接口）

- `500`：服务器内部错误

**业务自定义错误码**（放在返回 JSON 的 `code` 字段中）：

- `1001`：座位已被预约（并发锁座失败）
- `1002`：预约时间不在可选范围内（如跨天）
- `1003`：当前订单状态不允许此操作（如未到时间签到）
- `1004`：暂离已超时，座位已释放
- `1005`：内部服务鉴权失败（内部服务 Token 校验失败，见 1.3）
- `1006`：座位当前状态不可用（如处于维护、停用），不含时段冲突（时段冲突返回 1001）
- `1007`：外部依赖服务调用失败（如网络超时或拿不到下游响应报文，HTTP 状态码返回 200。注：下游正常业务错误仍透传原码，如 1001/1006）
- `2001`：账号或密码错误
- `2002`：账号已被禁用
- `2003`：账号（学号）已存在

> 说明：`2001-2003` 为身份权限子系统专属业务码，已由成员1提供并统一并入本表。

### 1.3 鉴权机制

- **用户端接口**：请求头必须携带 `Authorization: Bearer <JWT_Token>`。

- **内部服务接口（供定时任务/跨服务内部调用）**：请求头必须携带 `X-Internal-Token`，其值从各服务配置文件读取（配置项 `internal.token`），本文示例值 `campus_studyroom_internal_2026` 仅作联调参考，禁止硬编码在源码中；前端不得持有该 Token。

- **内部 Token 校验方式**：本项目为 4 个独立 SpringBoot 工程，不存在统一网关。凡契约标注为内部接口的路径，由**各服务自身拦截器**统一校验：请求头 `X-Internal-Token` 必须与配置值一致，否则拒绝；校验失败返回业务码 `1005`。8081 由成员1落地，8082 由成员2落地，8083 由成员3落地，8084 由成员4落地。

- **跨域**：所有 Controller 必须加 `@CrossOrigin`。

### 1.4 跨节点服务地址配置规范（严禁硬编码）

开发时统一在 `application.yml` 中配置，代码使用 `@Value` 读取。联调参考 IP 如下：

- **节点A宿主机IP（身份/座位）**：`10.165.59.231`

- **节点B虚拟机IP（订单/通知）**：`192.168.56.101`

配置示例：

```yaml
remote:
  auth-service-url: http://10.165.59.231:8081
  seat-service-url: http://10.165.59.231:8082
  order-service-url: http://192.168.56.101:8083
  notify-service-url: http://192.168.56.101:8084

internal:
  token: campus_studyroom_internal_2026   # 联调参考值，各服务从外部配置读取，禁止硬编码
```

## 2. 全局配置接口（供前端读取，由订单子系统8083提供）

### 2.1 获取业务配置

- **路径**：`GET /api/config/business`

- **描述**：获取业务参数（如时长档位、签到截止时间等），用于前端渲染下拉框。前端从订单系统获取，座位和通知系统若需要阈值，通过内部接口调用订单系统获取。

- **返回示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "durationOptions": [1, 2, 3, 4, 5, 6, 7, 8],
    "signInDeadlineMinutes": 15,
    "tempLeaveLimitMinutes": 120
  }
}
```

---

## 3. 座位资源子系统接口（成员2 负责实现）

### 3.1 获取自习室列表

- **路径**：`GET /api/rooms`

- **入参**：`pageNum`, `pageSize`

- **出参**：自习室基本信息列表。

### 3.2 获取指定自习室的座位列表

- **路径**：`GET /api/rooms/{roomId}/seats`

- **出参**：包含座位号、座位状态（枚举词严格遵循需求：`可用`、`已预约`、`已占用`、`暂离中`、`维护`、`停用`）。

### 3.3 锁定座位（核心并发防重接口）

- **路径**：`POST /api/seats/{seatId}/lock`

- **请求头**：必须包含 `X-Internal-Token`。

- **描述**：供订单子系统（8083）在生成订单前调用。**订单子系统应用层必须预生成全局唯一 `orderId`**，该 `orderId` 仅作锁座与补偿关联标识，不要求订单已落库；锁座成功后才落库生成订单；锁座失败不落订单；落库失败必须补偿调用 3.4 释放。

- **入参**：

```json
{
  "orderId": 123456789,
  "startTime": "2026-10-25 14:00:00",
  "durationHours": 2
}
```

- **业务规则**：  
  （1）校验 `开始时间 + 时长 <= 预约开始日次日 00:00:00`。  
  （2）主判据为座位非维护/停用，且该时段无未释放占用记录；**当前粗状态不作为未来时段拒绝依据**。  
  （3）无冲突则插入占用记录，返回成功；时段冲突返回错误码 `1001`；状态不可用（维护/停用）返回 `1006`。  
  （4）**幂等性要求**：同一 `orderId` 重复锁座必须幂等。若存在未释放占用记录，返回原 `occupancyId`；若已释放或同 `orderId` 不同 `seatId`/时段，返回明确业务码且不得新增占用；重新预约必须使用新 `orderId`。

- **响应示例**：

```json
{
  "code": 200,
  "message": "锁座成功",
  "data": {
    "occupancyId": 1001,
    "seatId": 1,
    "orderId": 123456789
  }
}
```

### 3.4 释放座位（供订单/通知子系统内部调用）

- **路径**：`POST /api/internal/seats/{seatId}/release`

- **请求头**：必须包含 `X-Internal-Token`。

- **入参**：

```json
{
  "orderId": 123456789,
  "action": "CANCEL" 
}
```

*(注：action 用于标识释放原因，如 CANCEL(取消)、TIMEOUT(超时)、FINISH(完成)，便于座位端做记录)*

- **说明**：释放对应订单的占用记录，若无未释放记录且非维护/停用，座位状态回“可用”。
- **幂等性要求**：按 `orderId` 重复释放必须幂等，返回成功或明确业务码。

### 3.5 更新座位状态（供订单子系统内部调用）

- **路径**：`POST /api/internal/seats/{seatId}/status`

- **请求头**：必须包含 `X-Internal-Token`。

- **描述**：供成员3（订单系统）在用户签到、暂离、暂离返回等场景下，通知座位子系统更新当前座位的物理粗状态。**该接口只做状态同步，不负责业务阈值判断**。

- **入参**：

```json
{
  "status": "已占用",
  "orderId": 123456789
}
```

*(注：`status` 字段仅接收 `已占用` 或 `暂离中`，不能通过此接口直接置为 `可用`，释放座位必须走 3.4 接口)*

- **业务规则**：校验订单与座位的关联性，防止越权修改其他座位的状态。

- **幂等性要求**：按 `orderId` 重复更新必须幂等。

## 4. 其他子系统接口清单

### 4.1 身份权限子系统（8081）- 负责人：成员1

> **模块鉴权补充说明**：本模块接口遵循 1.3 鉴权机制，用户端接口必须携带 `Authorization: Bearer <JWT_Token>`；若 Token 缺失、格式非法、签名无效或过期，统一返回 HTTP `401`；请求参数缺失/类型错误/格式错误返回 HTTP `400`；普通学生越权调用管理员接口，统一返回 HTTP `403`。

#### 4.1.1 学生用户注册

- **路径**：`POST /api/auth/register`
- **描述**：学生填写学号（作为登录账号）、密码、姓名注册，密码需加密存储。
- **入参**：

```json
{
  "username": "20230001",
  "password": "Password123!",
  "realName": "张三"
}
```

- **出参**：

```json
{
  "code": 200,
  "message": "注册成功",
  "data": {
    "userId": 1001
  }
}
```

- **业务前置条件**：`username`（学号）全局唯一。若账号已存在，返回业务自定义错误码 `2003`（详见 1.2 补充表）。

#### 4.1.2 用户登录

- **路径**：`POST /api/auth/login`

- **描述**：账号密码登录，签发 JWT 令牌。

- **入参**：

```json
{
 "username": "20230001",
 "password": "Password123!"
}
```

- **出参**：

```json
{
 "code": 200,
 "message": "登录成功",
 "data": {
 "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
 "tokenType": "Bearer",
 "expiresIn": 7200,
 "role": "STUDENT"
 }
}
```

- **业务前置条件**：账号必须存在且状态正常（未被禁用）。账号或密码错误返回业务码 `2001`；账号被禁用返回业务码 `2002`。

#### 4.1.3 获取当前登录用户信息

- **路径**：`GET /api/auth/me`

- **描述**：前端获取当前登录用户的基本信息（用于页面右上角展示）。

- **请求头**：`Authorization: Bearer <JWT_Token>`

- **出参**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "userId": 1001,
    "username": "20230001",
    "realName": "张三",
    "role": "STUDENT"
  }
}
```

#### 4.1.4 管理员分配用户角色（对应需求 FR-ADM-06）

- **路径**：`PUT /api/admin/users/{userId}/role`

- **描述**：管理员修改指定用户的系统角色（如将学生升级为管理员）。

- **请求头**：`Authorization: Bearer <JWT_Token>`（当前登录 Token 必须具备 ADMIN 角色权限）

- **入参**（路径参数 + Body）：

- `{userId}` (Long, 路径参数)：被修改角色的用户ID

```json
{
  "roleId": 2
}
```

- **出参**：

```json
{
  "code": 200,
  "message": "角色分配成功",
  "data": {}
}
```

- **业务前置条件**：仅管理员可调用。若普通学生使用其 Token 调用此接口，后端拦截器直接拦截并返回 HTTP 状态码 `403`。

#### 4.1.5 管理员分页查询用户列表（对应需求 FR-ADM-06 配套页面）

- **路径**：`GET /api/admin/users`

- **描述**：管理员分页查看系统内所有用户列表，支持按学号模糊搜索。

- **请求头**：`Authorization: Bearer <JWT_Token>`（当前登录 Token 必须具备 ADMIN 角色权限）

- **入参**（Query 参数）：

- `pageNum` (int, 必填，默认 1)

- `pageSize` (int, 必填，默认 10)

- `username` (string, 选填，用于模糊搜索)

- **出参**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "total": 15,
    "list": [
      {
        "userId": 1001,
        "username": "20230001",
        "realName": "张三",
        "role": "STUDENT",
        "status": 1
      }
    ]
  }
}
```

### 4.2 订单流转子系统（8083）- 负责人：成员3

**调用座位子系统接口通用说明**

本子系统调用座位资源子系统 `lock / release / status` 接口均携带内部鉴权头`X-Internal-Token`；lock请求体为`{orderId, startTime, durationHours}`，release请求体为`{orderId, action}`，status请求体为`{status, orderId}`；座位合法业务错误进行透传，网络无响应返回1007（依赖契约V1.1新增）。

#### 4.2.1 创建预约下单

- **路径**：`POST /api/orders/reserve`
- **描述**：学生发起座位预约；服务端预生成orderId，锁座请求携带该orderId；锁座成功才生成【待签到】订单，锁座失败不生成订单。锁座成功若本地订单落库失败，使用同一orderId执行座位release，action=CANCEL做补偿释放。
- **鉴权说明**：用户接口，请求头携带 `Authorization: Bearer <JWT_Token>`
- **入参**：

```json
{
  "seatId": 1,
  "startTime": "2026-10-25 14:00:00",
  "durationHours": 2
}
```

- **业务规则**：

(1)校验 `durationHours` 属于本地 `BIZ_CONFIG.durationOptions`；生产默认档位1‑8，测试环境可配置缩短。

(2)校验 `startTime` >= 当前系统时间；

(3)校验 `startTime + durationHours <= 预约开始日次日 00:00:00`，不满足返回业务错误码 `1002`；

(4)远程调用座位子系统锁座接口 `POST /api/seats/{seatId}/lock`；锁座返回1001/1006错误时直接透传错误，不生成订单；网络层面无法获取座位子系统响应则返回业务错误码=1007（HTTP状态码200），不生成订单。

(5)锁座成功，尝试生成订单；**若订单落库失败，调用座位释放接口 `POST /api/internal/seats/{seatId}/release`，action传值 `CANCEL` 做补偿；不返回成功**；

(6) 落库成功生成订单，订单状态为待签到；异步调用通知子系统消息推送接口 `POST /api/internal/notify/push`，`eventType` 传 `RESERVE_SUCCESS`，`dedupKey` 传 `RESERVE_SUCCESS:{orderId}` 推送预约成功消息，推送失败仅记录日志，不阻塞订单主流程。

- **响应示例**：

```json
{
  "code": 200,
  "message": "预约下单成功",
  "data": {
    "orderId": 10001,
    "seatId": 1,
    "status": "待签到",
    "startTime": "2026-10-25 14:00:00",
    "endTime": "2026-10-25 16:00:00"
  }
}
```

#### 4.2.2 订单签到

- **路径**：`POST /api/orders/{orderId}/sign-in`

- **描述**：学生执行订单签到；校验签到守卫条件；**先调用座位子系统状态更新接口，调用成功后再更新本地订单状态**；座位接口调用失败（网络超时/下游无响应）则订单状态保持原样，返回业务错误码=1007（HTTP状态码200），交由定时任务后续补偿。

- **鉴权说明**：用户接口，请求头携带 `Authorization: Bearer <JWT_Token>`

- **入参**：路径变量 `orderId`，无请求体

- **业务规则**：

(1)校验该订单属于当前登录用户；

(2)校验订单状态必须为【待签到】，否则返回业务错误码 `1003`；

(3)签到守卫条件：`预约开始时间 ≤ 当前时间 ≤ 预约开始时间 + 签到截止时长`；不满足条件返回业务错误码 `1003`；

(4)先远程调用座位子系统接口 `POST /api/internal/seats/{seatId}/status`，设置座位状态为“已占用”；座位子系统返回业务错误则直接透传code与message，不修改订单状态；

(5)座位接口调用成功，才更新本地订单状态为已签到；座位接口网络层面调用失败、无法获取响应，订单状态不变，返回业务错误码=1007（HTTP状态码200），依赖定时任务做后续补偿。

- **响应示例**：

```json
{
  "code": 200,
  "message": "签到成功",
  "data": {
    "orderId": 10001,
    "status": "已签到",
    "seatStatus": "已占用"
  }
}
```

#### 4.2.3 取消预约

- **路径**：`POST /api/orders/{orderId}/cancel`

- **描述**：学生取消待签到状态的预约；**先调用座位子系统释放接口，调用成功后再更新本地订单状态**；座位释放网络调用失败，订单状态不变，返回业务错误码=1007（HTTP状态码200），由定时任务补偿。

- **鉴权说明**：用户接口，请求头携带 `Authorization: Bearer <JWT_Token>`

- **入参**：路径变量 `orderId`，无请求体

- **业务规则**：

(1)校验该订单属于当前登录用户；

(2)校验订单状态必须为【待签到】，否则返回业务错误码 `1003`；

(3)校验 `当前时间 ≤ 预约开始时间 + 签到截止时长`，不满足返回业务错误码`1003`，交由批量超时扫描处理；

(4)先远程调用座位子系统释放接口 `POST /api/internal/seats/{seatId}/release`，action传值 `CANCEL`；座位子系统返回业务错误则直接透传code与message，不修改订单状态；

(5)座位释放接口调用成功，修改本地订单状态为已取消；网络层面调用失败，订单状态维持原状，返回业务错误码=1007（HTTP状态码200），依赖定时任务补偿。

- **响应示例**：

```json
{
  "code": 200,
  "message": "取消预约成功",
  "data": {
    "orderId": 10001,
    "status": "已取消"
  }
}
```

#### 4.2.4 订单执行暂离

- **路径**：`POST /api/orders/{orderId}/temp-leave`

- **描述**：学生对已签到订单执行暂离；增加重复暂离校验；**先调用座位子系统状态更新接口，调用成功再更新本地订单暂离时间**；座位接口网络调用失败，本地不做变更，返回业务错误码=1007（HTTP状态码200），定时任务补偿。

- **鉴权说明**：用户接口，请求头携带 `Authorization: Bearer <JWT_Token>`

- **入参**：路径变量 `orderId`，无请求体

- **业务规则**：

(1)校验该订单属于当前登录用户；

(2)校验订单状态必须为【已签到】，否则返回业务错误码 `1003`；

(3)校验 `tempLeaveStartTime` 为空，即当前未处于暂离中；若已暂离，返回业务错误码 `1003`；

(4)校验 `当前时间 < 预约结束时间`，否则返回业务错误码`1003`；

(5)先远程调用座位子系统接口 `POST /api/internal/seats/{seatId}/status`，设置座位状态为“暂离中”；座位子系统返回业务错误则直接透传code与message，不修改本地字段；

(6)座位接口调用成功，记录暂离开始时间；网络层面调用失败，本地不更新字段，返回业务错误码=1007（HTTP状态码200），依赖定时任务补偿。

- **响应示例**：

```json
{
  "code": 200,
  "message": "暂离操作成功",
  "data": {
    "orderId": 10001,
    "status": "已签到",
    "seatStatus": "暂离中",
    "tempLeaveStartTime": "2026-10-25 14:30:00"
  }
}
```

#### 4.2.5 暂离返回（重新签到）

- **路径**：`POST /api/orders/{orderId}/return-back`

- **描述**：学生暂离之后返回，执行重新签到；做三段业务校验：使用到期优先、暂离超时、正常返回；**优先调用座位子系统接口，接口成功后再变更订单状态、清空暂离时间；超时分支异步推送消息**。

- **鉴权说明**：用户接口，请求头携带 `Authorization: Bearer <JWT_Token>`

- **入参**：路径变量 `orderId`，无请求体

- **业务规则**：

(1)校验该订单属于当前登录用户；

(2)校验订单状态必须为【已签到】且处于暂离中（`tempLeaveStartTime`不为空），否则返回业务错误码 `1003`；

(3)三段校验逻辑：

- ① 如果当前时间 ≥ 预约结束时间：
  先调用座位子系统release接口，action传值`FINISH`；座位子系统返回业务错误则直接透传code与message，本地不做变更；座位调用成功；订单转为已完成；清空`tempLeaveStartTime`；不生成违约记录；不推送消息；网络调用失败，本地不变更，返回业务错误码=1007（HTTP状态码200），定时任务补偿。

- ② 如果当前时间 > 暂离开始时间+暂离保留上限 且 当前时间＜预约结束时间：  
  先调用座位release接口，action传值`TIMEOUT`；座位子系统返回业务错误则直接透传code与message，本地不做变更；座位调用成功；订单转为暂离超时释放；生成违约记录（类型`暂离超时`）；清空`tempLeaveStartTime`；异步调用通知子系统接口 `POST /api/internal/notify/push`，`eventType` 传 `TEMP_LEAVE_TIMEOUT_RELEASE`，`dedupKey` 传 `TEMP_LEAVE_TIMEOUT_RELEASE:{orderId}`；推送失败仅记日志不阻塞；返回业务错误码`1004`；网络调用失败，本地不变更，返回业务错误码=1007（HTTP状态码200），定时任务补偿。

- ③ 其余情况为正常返回：
  先调用座位status接口设置为“已占用”；座位子系统返回业务错误则直接透传code与message，本地不做变更；座位调用成功；清空`tempLeaveStartTime`；订单保持已签到；网络调用失败，本地不变更，返回业务错误码=1007（HTTP状态码200），定时任务补偿。

- **响应示例（正常返回场景）**：

```json
{
  "code": 200,
  "message": "返回座位成功",
  "data": {
    "orderId": 10001,
    "status": "已签到",
    "seatStatus": "已占用",
    "tempLeaveStartTime": null
  }
}
```

- **响应示例（暂离超时分支）**：

```json
{
  "code": 1004,
  "message": "暂离已超时，座位已释放",
  "data": null
}
```

#### 4.2.6 主动离开签退

- **路径**：`POST /api/orders/{orderId}/leave`

- **描述**：学生主动结束使用；允许暂离状态下直接离开；无论是否满足暂离超时条件，均直接执行完成流程，不生成暂离超时违约记录；**先调用座位释放接口，成功后更新订单状态并清空暂离时间**；座位网络调用失败本地不变更，返回业务错误码=1007（HTTP状态码200），定时任务补偿。

- **鉴权说明**：用户接口，请求头携带 `Authorization: Bearer <JWT_Token>`

- **入参**：路径变量 `orderId`，无请求体

- **业务规则**：

（1）校验该订单属于当前登录用户；

（2）校验订单状态必须为【已签到】，否则返回业务错误码 `1003`；

（3）主动离开优先于暂离超时判定：即使 `tempLeaveStartTime` 不为空且当前时间 > 暂离开始时间+暂离保留上限，也一律按主动离开处理，转已完成、不生成违约记录；

（4) 先远程调用座位子系统释放接口 `POST /api/internal/seats/{seatId}/release`，action传值 `FINISH`；座位子系统返回业务错误则直接透传code与message，订单与暂离字段维持原状；

(5) 座位接口调用成功：修改订单状态为已完成，清空`tempLeaveStartTime`，不生成违约记录；网络层面调用失败，订单与暂离字段维持原状，返回业务错误码=1007（HTTP状态码200），依赖定时任务补偿。

- **响应示例**：

```json
{
  "code": 200,
  "message": "主动离开成功",
  "data": {
    "orderId": 10001,
    "status": "已完成"
  }
}
```

#### 4.2.7 查询我的订单分页列表

- **路径**：`GET /api/orders/my-list`

- **描述**：分页查询当前登录学生自己的全部订单。

- **鉴权说明**：用户接口，请求头携带 `Authorization: Bearer <JWT_Token>`

- **入参**：url查询参数：`pageNum`、`pageSize`

- **业务规则**：
1. 根据JWT解析当前登录用户id，只查询属于该用户的订单；

2. 做分页处理。
- **响应示例**：

```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "total": 2,
    "pageNum": 1,
    "pageSize": 10,
    "list": [
      {
        "orderId":10001,
        "seatId":1,
        "status":"待签到",
        "startTime":"2026-10-25 14:00:00",
        "endTime":"2026-10-25 16:00:00",
        "tempLeaveStartTime":null
      }
    ]
  }
}
```

#### 4.2.8 查询订单详情

- **路径**：`GET /api/orders/{orderId}/detail`
- **描述**：查询单条订单完整详情。
- **鉴权说明**：用户接口，请求头携带 `Authorization: Bearer <JWT_Token>`
- **入参**：路径变量 `orderId`，无请求体
- **业务规则**：校验订单属于当前登录用户；否则返回403无权限。
- **响应示例**：

```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "orderId":10001,
    "seatId":1,
    "status":"待签到",
    "startTime":"2026-10-25 14:00:00",
    "endTime":"2026-10-25 16:00:00",
    "tempLeaveStartTime":null
  }
}
```

#### 4.2.9 管理员分页查询全部订单

- **路径**：`GET /api/admin/orders`

- **描述**：供管理员分页查询系统全部订单，支持多条件过滤；普通学生访问返回403无权限。

- **鉴权说明**：管理员接口，请求头携带 `Authorization: Bearer <JWT_Token>`

- **入参**：url查询参数：`pageNum`、`pageSize`、`status`、`userId`、`seatId`、`startDate`、`endDate`

- **业务规则**：
1. 校验登录用户为管理员角色；非管理员返回403；

2. 按传入条件做分页过滤；未传入过滤条件则返回全部。
- **响应示例**：

```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "total": 12,
    "pageNum": 1,
    "pageSize": 10,
    "list": [
      {
        "orderId":10001,
        "seatId":1,
        "userId":1002,
        "status":"待签到",
        "startTime":"2026-10-25 14:00:00",
        "endTime":"2026-10-25 16:00:00",
        "tempLeaveStartTime":null
      }
    ]
  }
}
```

#### 4.2.10 管理员分页查询违约记录

- **路径**：`GET /api/admin/violations`

- **描述**：供管理员分页查询违约记录；普通学生访问返回403无权限。

- **鉴权说明**：管理员接口，请求头携带 `Authorization: Bearer <JWT_Token>`

- **入参**：url查询参数：`pageNum`、`pageSize`、`userId`、`orderId`、`type`、`startDate`、`endDate`

- **业务规则**：
1. 校验登录用户为管理员角色；非管理员返回403；

2. 按条件分页过滤，无过滤条件返回全部违约记录。
- **响应示例**：

```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "total": 3,
    "pageNum": 1,
    "pageSize": 10,
    "list": [
      {
        "violationId":5001,
        "orderId":10002,
        "userId":1003,
        "type":"暂离超时",
        "violateTime":"2026-10-25 15:40:00",
        "reason":"暂离超过暂离保留上限未返回"
      }
    ]
  }
}
```

#### 4.2.11 内部‑读取业务配置（供通知子系统调用）

- **路径**：`GET /api/internal/config/business`
- **描述**：仅供监控通知子系统(8084)调用，获取业务阈值；前端禁止访问。
- **鉴权说明**：内部接口，请求头必须携带 `X-Internal-Token: campus_studyroom_internal_2026`；前端禁止访问。鉴权失败返回业务错误码`1005`。
- **入参**：无请求体
- **业务规则**：读取本地BIZ_CONFIG表配置数据返回。
- **备注**：`max_duration_hours` 仅后端内部校验用、不在契约 4.2.11 对外返回。
- **响应示例**：

```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "durationOptions": [1, 2, 3, 4, 5, 6, 7, 8],
    "signInDeadlineMinutes": 15,
    "tempLeaveLimitMinutes": 120,
    "scanIntervalSeconds": 60
  }
}
```

#### 4.2.12 内部‑批量触发订单超时处理

- **路径**：`POST /api/internal/orders/batch-handle-timeout`

- **描述**：仅供监控通知子系统（8084）定时任务调用；扫描订单，执行到期、超时未签到、暂离超时处理；**禁止前端调用。业务阈值读取订单子系统本地BIZ_CONFIG配置，调用方不传入判断阈值。**

- **鉴权说明**：内部接口，请求头必须携带 `X-Internal-Token: campus_studyroom_internal_2026`；前端禁止访问。鉴权失败返回业务错误码`1005`。

- **入参**：无请求体

- **业务规则**：

(1)读取本地业务配置（签到截止时长、暂离保留上限）；

(2)只处理非终态订单；已为终态订单直接跳过，保证幂等；订单状态更新使用条件更新（where status=非终态）；订单状态变更与违约记录生成放在本地事务；重复扫描不会重复生成违约、不会重复触发消息；座位释放失败跳过本轮，等待下一轮重试。

(3)扫描分支严格按使用到期优先：

- ① 使用到期：订单状态=已签到（含暂离中）且 `当前时间 >= endTime`；先调用座位释放接口 `action=FINISH`；座位调用成功，订单置`已完成`，清空`tempLeaveStartTime`，不生成违约记录；异步调用通知子系统接口 `POST /api/internal/notify/push`，`eventType` 传 `USE_EXPIRED_COMPLETE`，`dedupKey` 传 `USE_EXPIRED_COMPLETE:{orderId}`，推送使用到期提示消息，推送失败仅记日志。座位释放失败记录日志，计入`seatReleaseFailed`，该订单跳过本次处理；
- ② 超时未签到：订单状态=待签到 且 `当前时间 > startTime + 签到截止时长`；先调用座位释放接口 `action=TIMEOUT`；座位调用成功；订单置`超时释放`；生成违约记录（类型`超时未签到`）；异步调用通知子系统接口 `POST /api/internal/notify/push`，`eventType` 传 `SIGN_IN_TIMEOUT_RELEASE`，`dedupKey` 传 `SIGN_IN_TIMEOUT_RELEASE:{orderId}`；推送失败仅记日志；座位释放失败记录日志，计入`seatReleaseFailed`，订单跳过本次处理；
- ③ 暂离超时：订单状态=已签到，`tempLeaveStartTime`不为空，`当前时间 > tempLeaveStartTime + 暂离保留上限` 且 `当前时间 < endTime`；先调用座位释放接口`action=TIMEOUT`；座位调用成功；订单置`暂离超时释放`；生成违约记录（类型`暂离超时`）；异步调用通知子系统接口 `POST /api/internal/notify/push`，`eventType` 传 `TEMP_LEAVE_TIMEOUT_RELEASE`，`dedupKey` 传 `TEMP_LEAVE_TIMEOUT_RELEASE:{orderId}`；推送失败仅记日志；座位释放失败记录日志，计入`seatReleaseFailed`，订单跳过本次处理。

（（4）座位释放失败不阻塞整体扫描流程，记录日志统计失败数；消息推送失败仅记录日志，不阻塞扫描主流程。
（5）统计输出：总处理数、签到超时释放数、暂离超时释放数、到期完成数、生成违约记录数、座位释放失败数、触发消息推送数。统计字段为运行时调度简报，不持久化存储。

**响应示例**：

```json
{
  "code": 200,
  "message": "批量超时扫描执行完成",
  "data": {
    "handleCount": 2,
    "signInTimeoutReleased": 1,
    "tempLeaveTimeoutReleased": 0,
    "expiredCompleted": 1,
    "violationCreated": 1,
    "seatReleaseFailed": 0,
    "messagePushTriggered": 1 
  }
}
```

### 4.3 监控通知子系统（8084）- 负责人：成员4

> **模块说明**：本子系统只负责定时触发与消息生成/推送，不存在业务阈值、不直连 `order_db` / `room_seat_db`。定时任务按配置周期调用订单子系统 `POST /api/internal/orders/batch-handle-timeout`（4.2.12）触发超时/暂离超时/到期处理；订单状态迁移、违约记录、座位释放由订单子系统与座位子系统完成。本子系统不调用座位 3.4/3.5。用户端接口遵循 1.3 鉴权；内部接口必须携带 `X-Internal-Token`，校验失败返回业务码 `1005`。

#### 4.3.1 查询本人消息列表

- **路径**：`GET /api/notify/messages`
- **鉴权**：用户接口，请求头 `Authorization: Bearer <JWT_Token>`
- **描述**：分页查询当前登录用户的消息，按创建时间倒序。
- **入参**（Query）：

| 参数        | 类型     | 必填  | 说明                      |
| --------- | ------ | --- | ----------------------- |
| pageNum   | int    | 否   | 默认 1                    |
| pageSize  | int    | 否   | 默认 10，最大 50             |
| status    | string | 否   | `UNREAD` / `READ`，不传查全部 |
| eventType | string | 否   | 按事件类型过滤，见 4.3.4 枚举      |

- **业务规则**：从 JWT 解析 `userId`，只查本人消息；非法 Token 返回 HTTP 401。
- **响应示例**：

```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "total": 2,
    "pageNum": 1,
    "pageSize": 10,
    "list": [
      {
        "messageId": 1001,
        "eventType": "SIGN_IN_TIMEOUT_RELEASE",
        "orderId": 10001,
        "title": "预约超时释放",
        "content": "您预约的座位因超时未签到已释放",
        "status": "UNREAD",
        "bizTime": "2026-10-25 14:20:00",
        "createTime": "2026-10-25 14:20:02",
        "readTime": null
      }
    ]
  }
}
```

#### 4.3.2 标记消息已读

- **路径**：`POST /api/notify/messages/read`

- **鉴权**：用户接口，请求头 `Authorization: Bearer <JWT_Token>`

- **描述**：将当前用户指定消息标记为已读；重复标记幂等。

- **入参**：

```json
{
  "messageIds": [1001, 1002]
}
```

- **业务规则**：
  
  1. `messageIds` 必填且非空，否则返回 HTTP 400；
  
  2. 只更新属于当前用户且 `status=UNREAD` 的消息，不属于当前用户的消息忽略，不报错；
  
  3. 返回实际更新条数。

- **响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "updated": 2
  }
}
```

#### 4.3.3 查询未读消息数

- **路径**：`GET /api/notify/messages/unread-count`

- **鉴权**：用户接口，请求头 `Authorization: Bearer <JWT_Token>`

- **描述**：供前端消息红点使用。

- **入参**：无

- **响应示例**：

```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "unreadCount": 3
  }
}
```

#### 4.3.4 内部消息推送（供订单等子系统调用）

- **路径**：`POST /api/internal/notify/push`

- **鉴权**：内部接口，请求头必须携带 `X-Internal-Token`；前端禁止持有。

- **描述**：订单子系统在预约成功、超时释放、暂离超时释放、到期完成等事件后调用；通知子系统只入库并推送，不反查订单、不调座位接口。

- **入参**：

```json
{
  "eventType": "SIGN_IN_TIMEOUT_RELEASE",
  "userId": 1002,
  "orderId": 10001,
  "title": "预约超时释放",
  "content": "您预约的座位因超时未签到已释放",
  "bizTime": "2026-10-25 14:20:00",
  "dedupKey": "SIGN_IN_TIMEOUT_RELEASE:10001"
}
```

| 字段        | 必填  | 说明                                           |
| --------- | --- | -------------------------------------------- |
| eventType | 是   | 事件类型，见下方枚举                                   |
| userId    | 是   | 接收用户 ID                                      |
| orderId   | 否   | 关联订单 ID；系统消息可为空                              |
| title     | 是   | 消息标题，≤100 字符                                 |
| content   | 是   | 消息内容，≤500 字符                                 |
| bizTime   | 是   | 业务发生时间，格式 `yyyy-MM-dd HH:mm:ss`              |
| dedupKey  | 是   | 幂等键，建议 `eventType:orderId`；重复调用返回原 messageId |

- **事件类型枚举（建议，需成员3确认后冻结）**：

| 枚举                           | 触发场景                      |
| ---------------------------- | ------------------------- |
| `RESERVE_SUCCESS`            | 预约成功（订单 4.2.1）            |
| `SIGN_IN_TIMEOUT_RELEASE`    | 超时未签到释放（订单 4.2.12②）       |
| `TEMP_LEAVE_TIMEOUT_RELEASE` | 暂离超时释放（订单 4.2.5②、4.2.12③） |
| `USE_EXPIRED_COMPLETE`       | 使用到期完成（订单 4.2.12①，可选推送）   |
| `SYSTEM`                     | 系统消息（预留）                  |

- **业务规则**：
  
  1. `dedupKey` 唯一，重复推送返回原 `messageId` 且 `duplicated=true`，不重复入库；
  
  2. 推送失败只记录 `notify_push_log`，不向调用方抛异常；调用方按“推送失败不阻塞主流程”处理；
  
  3. 参数缺失/格式错误返回 HTTP 400；内部 Token 错误返回业务码 `1005`。

- **响应示例**：

```json
{
  "code": 200,
  "message": "推送成功",
  "data": {
    "messageId": 1001,
    "duplicated": false
  }
}
```

重复调用示例：

```json
{
  "code": 200,
  "message": "消息已存在",
  "data": {
    "messageId": 1001,
    "duplicated": true
  }
}
```

#### 4.3.5 内部手动触发补偿扫描（可选，测试/运维用）

- **路径**：`POST /api/internal/notify/compensate`

- **鉴权**：内部接口，请求头 `X-Internal-Token`

- **描述**：手动触发一次与定时任务相同的补偿扫描，内部调用订单 `POST /api/internal/orders/batch-handle-timeout`（4.2.12）。用于成员6容错测试恢复后立即验证，不必等定时周期；定时任务本身自动执行，不依赖本接口。

- **入参**：无

- **业务规则**：本接口不处理订单、不释放座位，只透传订单 4.2.12 的处理统计；订单返回业务错误码时透传。

- **响应示例**：

```json
{
  "code": 200,
  "message": "补偿扫描触发完成",
  "data": {
    "handleCount": 2,
    "signInTimeoutReleased": 1,
    "tempLeaveTimeoutReleased": 0,
    "expiredCompleted": 1,
    "violationCreated": 1,
    "seatReleaseFailed": 0,
    "messagePushTriggered": 1
  }
}
```

#### 4.3.6 定时任务说明（非接口）

- 通知子系统定时任务按本服务配置的扫描周期执行，调用订单 `POST /api/internal/orders/batch-handle-timeout`（4.2.12）触发处理；

- 不自行计算业务阈值，不调用座位 3.4/3.5；

- 订单 4.2.12 内部异步调用 4.3.4 完成消息推送；推送失败仅记通知侧日志，不影响订单主流程；

- 扫描周期从本服务配置 `notify.scan.interval` 读取；如需与订单配置一致，可调用 4.2.11 读取 `scanIntervalSeconds` 作为参考，最终以成员2/3确认口径为准。

## 5. 内部接口调用链路清单（基线定调）

为了保证分布式架构下各子系统的边界清晰，严格遵循“禁止跨库直连”与“谁调用谁主导联调”的原则，内部跨节点调用链路基线如下：

（1）**订单流转子系统 (8083) 调用 座位资源子系统 (8082)**

- **锁座**：`POST /api/seats/{seatId}/lock` (订单应用层预生成 orderId 传入，锁座成功后才落订单)
- **释放座位**：`POST /api/internal/seats/{seatId}/release` (幂等)
- **状态同步**：`POST /api/internal/seats/{seatId}/status` (仅接收“已占用/暂离中”，幂等)
- *(注：上述接口均由成员2定义，成员3负责发起调用)*

（2）**订单流转子系统 (8083) 调用 监控通知子系统 (8084)**

- **消息推送**：订单状态变更时，异步调用通知子系统接口。推送失败不可阻塞订单主流程。
- *(注：具体接口契约由成员4定义，成员3负责发起调用)*

（3）**监控通知子系统 (8084) 调用 订单流转子系统 (8083)**

- **配置读取**：`GET /api/internal/config/business` (读取业务阈值，严禁自行建表存储)
- **批量扫描触发**：`POST /api/internal/orders/batch-handle-timeout` (触发超时/到期处理)
- *(注：具体接口契约由成员3定义，成员4负责发起调用)*
