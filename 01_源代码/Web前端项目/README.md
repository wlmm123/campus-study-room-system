# 校园自习室分布式预约管理系统 - Web 前端

本项目为“校园自习室分布式预约管理系统”的 Web 前端工程。采用 **Vue 3 + Vite + Element Plus** 构建，支持学生端在线预约、签到、暂离及管理员端座位强制维护等功能，界面采用独特的“木质校园风”设计。

## 一、 技术栈
- **核心框架**：Vue 3 (Composition API)
- **构建工具**：Vite
- **UI 组件库**：Element Plus
- **路由管理**：Vue Router
- **网络请求**：Axios（封装了统一拦截器与 Token 鉴权）
- **时间处理**：Day.js

## 二、 核心功能模块
1. **统一登录鉴权**：对接 8081 端口，支持学生与管理员角色区分登录，Token 本地持久化存储，路由守卫拦截未登录访问。
2. **学生端 - 查座预约**：严格按照业务契约，先选择“使用时段”，再查询座位，根据后端返回的 `bookable` 字段（而非 `status`）渲染座位可约性（绿色可约、灰色不可约）。
3. **学生端 - 我的订单**：状态机全流程展示，支持签到、暂离、主动离开、取消预约。按钮状态根据订单当前状态动态渲染。
4. **管理员端 - 座位管理**：支持设置座位为维护状态。内置了捕获后端 `1006` 错误码的逻辑（当座位有未来预约时，弹窗提示管理员强制取消受影响的订单）。

## 三、 本地开发与启动指南

### 1. 安装依赖
在项目根目录下运行以下命令：
    npm install

### 2. 配置后端接口代理
打开根目录下的 `vite.config.js`，根据你当前真实的 Java 后端环境，修改 `proxy` 的 `target`：
- 节点 A（8081 身份权限、8082 座位资源）
- 节点 B（8083 订单流转）

找到 `vite.config.js` 里的 `proxy` 配置，格式如下：
    proxy: {
      '/api/auth': { target: 'http://127.0.0.1:8081', changeOrigin: true },
      '/api/rooms': { target: 'http://127.0.0.1:8082', changeOrigin: true },
      '/api/admin/seats': { target: 'http://127.0.0.1:8082', changeOrigin: true },
      '/api/orders': { target: 'http://127.0.0.1:8083', changeOrigin: true }
    }
*(注：如果后端部署在其他局域网机器上，请将 `127.0.0.1` 替换为对应节点的真实局域网 IP)*

### 3. 启动开发服务器
在终端运行以下命令：
    npm run dev

启动成功后，浏览器访问终端中输出的本地地址（通常为 `http://localhost:5173`）即可预览。

## 四、 生产环境打包
如果需要进行双节点部署，请在终端执行以下命令：
    npm run build

构建完成后，项目根目录会自动生成 `dist` 文件夹。**请将 `dist` 文件夹内的所有内容**交给部署负责人（成员6），放置于节点 A 的 Nginx 或静态服务器中。

## 五、 目录结构说明

    Web前端项目/
    ├── src/
    │   ├── api/          # 所有发往后端 8081/8082/8083 的接口封装
    │   ├── assets/       # 静态资源（校徽、背景图等）
    │   ├── router/       # Vue Router 路由配置及权限守卫
    │   ├── utils/        # 工具类（包含 Axios 统一拦截器 request.js）
    │   ├── views/        # 页面视图
    │   │   ├── admin/    # 管理员端页面 (SeatManage.vue)
    │   │   ├── student/  # 学生端页面 (RoomList.vue, MyOrders.vue)
    │   │   ├── Layout.vue# 全局导航框架
    │   │   └── Login.vue # 登录页
    │   ├── App.vue       # 根组件
    │   └── main.js       # 项目入口文件
    ├── vite.config.js    # Vite 配置与跨域代理
    └── package.json      # 依赖清单

## 六、 注意事项
- 前端所有的请求统一走 `/api` 前缀，由 `vite.config.js` 解决开发环境的跨域问题。生产环境部署时，Nginx 也需要配置相应的反向代理。
- 管理员及学生测试账号密码，详见后端 `auth_db` 的初始化脚本。