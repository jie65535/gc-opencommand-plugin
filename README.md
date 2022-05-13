# gc-opencommand-plugin

一个为第三方客户端开放GC命令执行接口的插件

# 服务端安装
1. 在 Release 下载 `jar`
2. 放入 `plugins` 文件夹即可

# 玩家使用流程
1. 在客户端中填写服务地址，确认是否支持
2. 填写UID，发送验证码
3. 将游戏内收到的**4位整数验证码**填入客户端校验
4. 享受便利！

# 客户端请求流程
1. `ping` 确认是否支持 `opencommand` 插件
2. `sendCode` 向指定玩家发送验证码（1分钟内不允许重发），保存返回的 `token`
3. 使用 `token` 和**4位整数验证码**发送 `verify` 校验
4. 如果验证通过，可以使用该 `token` 执行 `command` 动作

---

# API `/opencommand/api`
示例
```
https://127.0.0.1/opencommand/api
```

# Request 请求
```java
public final class JsonRequest {
    public String token = "";
    public String action = "";
    public Object data = null;
}
```

# Response 响应
```java
public final class JsonResponse {
    public int retcode = 200;
    public String message = "success";
    public Object data;
}
```

# Actions 动作
## `ping`
data = null

## `sendCode`
### Request
data = uid (int)
### Response
data = token (string)

## `verify` 要求 `token`
### Request
data = code (int)
### Response
#### Success:
code = 200
#### Verification failed:
code = 400

## `command` 要求 `token`
### Request
data = command (string)
### Response
data = message (string)
