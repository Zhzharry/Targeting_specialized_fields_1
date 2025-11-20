# static 前端Vue应用目录

## 文件夹作用

此文件夹用于存放前端Vue应用的静态资源文件，包括Vue组件、HTML、CSS、JavaScript、图片、字体等文件。这是前后端分离架构中的前端部分。

## 运行配置

- **前端运行端口**：5173
- **后端API地址**：http://localhost:5000/api
- **前后端分离**：前端Vue应用独立运行在5173端口，通过API调用后端服务

## 对应页面/功能

此目录包含完整的Vue前端应用，为所有页面提供前端界面和交互功能。

## 文件组织建议

```
static/
├── css/           # 样式文件
│   ├── common.css    # 通用样式
│   ├── theme.css     # 主题样式
│   └── ...
├── js/            # JavaScript文件
│   ├── utils.js      # 工具函数
│   ├── constants.js # 常量定义
│   └── ...
├── images/        # 图片资源
│   ├── logo.png      # Logo图片
│   ├── icons/        # 图标文件
│   └── ...
├── fonts/         # 字体文件
│   └── ...
└── uploads/       # 上传文件目录
    └── ...
```

## 代码框架

### 通用CSS样式框架
```css
/* css/common.css */
/* 全局样式重置 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

/* 通用容器样式 */
.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

/* 通用按钮样式 */
.btn {
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.btn-primary {
  background-color: #409eff;
  color: white;
}

/* 通用表格样式 */
.table {
  width: 100%;
  border-collapse: collapse;
}

.table th,
.table td {
  padding: 12px;
  border: 1px solid #ddd;
  text-align: left;
}
```

### 工具函数框架
```javascript
// js/utils.js
// 日期格式化
export function formatDate(date, format = 'YYYY-MM-DD') {
  // TODO: 实现日期格式化逻辑
}

// 防抖函数
export function debounce(func, wait) {
  let timeout
  return function executedFunction(...args) {
    const later = () => {
      clearTimeout(timeout)
      func(...args)
    }
    clearTimeout(timeout)
    timeout = setTimeout(later, wait)
  }
}

// 节流函数
export function throttle(func, limit) {
  let inThrottle
  return function(...args) {
    if (!inThrottle) {
      func.apply(this, args)
      inThrottle = true
      setTimeout(() => inThrottle = false, limit)
    }
  }
}

// 深拷贝
export function deepClone(obj) {
  // TODO: 实现深拷贝逻辑
}

// 生成唯一ID
export function generateId() {
  return Date.now().toString(36) + Math.random().toString(36).substr(2)
}
```

### 常量定义框架
```javascript
// js/constants.js
// API地址 - 后端Spring Boot运行在5000端口
export const API_BASE_URL = 'http://localhost:5000/api'

// 页面大小选项
export const PAGE_SIZES = [10, 20, 50, 100]

// 日期格式
export const DATE_FORMAT = 'YYYY-MM-DD'
export const DATETIME_FORMAT = 'YYYY-MM-DD HH:mm:ss'

// 状态码
export const HTTP_STATUS = {
  OK: 200,
  CREATED: 201,
  BAD_REQUEST: 400,
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
  NOT_FOUND: 404,
  INTERNAL_SERVER_ERROR: 500
}
```

## 文件组织建议（Vue应用）

```
static/
├── index.html          # 入口HTML文件
├── src/                # Vue源代码目录
│   ├── main.js        # Vue应用入口
│   ├── App.vue        # 根组件
│   ├── components/    # Vue组件
│   ├── views/         # 页面视图
│   ├── router/        # 路由配置
│   ├── store/         # 状态管理
│   └── api/           # API调用
├── public/            # 公共静态资源
│   ├── favicon.ico
│   └── ...
├── package.json       # 依赖配置
└── vite.config.js     # Vite配置（或vue.config.js）
```

## 启动说明

### 前端启动（端口5137）
```bash
# 安装依赖
npm install

# 启动开发服务器（运行在5137端口）
npm run dev
```

### 后端启动（端口5000）
```bash
# Spring Boot应用运行在5000端口
mvn spring-boot:run
```

## API调用配置

前端Vue应用需要配置API基础地址指向后端：

```javascript
// 在Vue应用的API配置文件中
const API_BASE_URL = 'http://localhost:5000/api'

// 使用axios调用后端API
axios.get(`${API_BASE_URL}/users`)
```

## 注意事项

- 前端Vue应用运行在 **5137端口**
- 后端Spring Boot API运行在 **5000端口**
- 确保后端CORS配置允许5137端口访问
- 前后端完全分离，通过RESTful API通信
- 开发时注意跨域问题，已在后端配置CORS支持

