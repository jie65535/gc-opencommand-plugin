# gc-opencommand-plugin

中文 | [English](README_en-US.md)

一个为第三方客户端开放GC命令执行接口的插件

## 服务端安装
1. 在 [Release](https://github.com/jie65535/gc-opencommand-plugin/releases) 下载 `jar`
2. 放入 `plugins` 文件夹即可
> 注意，如果出现以下错误：
> ```log
> INFO:PluginManager Enabling plugin: opencommand-plugin
> Exception in thread "main" java.lang.NoSuchMethodError: 'void emu.grasscutter.server.event.EventHandler.register(emu.grasscutter.plugin.Plugin)'
> at com.github.jie65535.opencommand.OpenCommandPlugin.onEnable(OpenCommandPlugin.java:49)
> at emu.grasscutter.plugin.PluginManager.lambda$enablePlugins$3(PluginManager.java:131)
> ```
> 请使用v1.2.1版本插件，因为该报错表示你的服务端是旧版！

## 控制台连接
1. 首次启动时，会在 `plugins` 目录下生成一个 `opencommand-plugin` 目录，打开并编辑 `config.json`
2. 设置 `consoleToken` 的值为你的连接秘钥，建议使用至少32字符的长随机字符串。
3. 重新启动服务端即可生效配置
4. 在客户端中选择控制台身份，并填写你的 `consoleToken` 即可以控制台身份运行指令

## 构建说明
1. 克隆仓库
2. 在目录下新建 `lib` 目录
3. 将 `grasscutter-1.1.x-dev.jar` 放入 `lib` 目录
4. `gradle build`

## 玩家使用流程
1. 在客户端中填写服务地址，确认是否支持
2. 填写UID，发送验证码
3. 将游戏内收到的**4位整数验证码**填入客户端校验
4. 享受便利！

## 客户端请求流程
1. `ping` 确认是否支持 `opencommand` 插件
2. `sendCode` 向指定玩家发送验证码（1分钟内不允许重发），保存返回的 `token`
3. 使用 `token` 和**4位整数验证码**发送 `verify` 校验
4. 如果验证通过，可以使用该 `token` 执行 `command` 动作

---

## `config.json`
```json
{
  // 控制台连接令牌
  "consoleToken": "",
  // 验证码过期时间（秒）
  "codeExpirationTime_S": 60,
  // 临时令牌过期时间（秒）
  "tempTokenExpirationTime_S": 300,
  // 授权令牌最后使用过期时间（小时）
  "tokenLastUseExpirationTime_H": 48,
  // 多服务器通信端口
  "socketPort": 5746,
  // 多服务器通信密钥
  "socketToken": "",
  // 多服务器Dispatch地址
  "socketHost": "127.0.0.1"
}
```


## API `/opencommand/api`
示例
```
https://127.0.0.1/opencommand/api
```

### Request 请求
```java
public final class JsonRequest {
    public String token = "";
    public String action = "";
    public Object data = null;
}
```

### Response 响应
```java
public final class JsonResponse {
    public int retcode = 200;
    public String message = "success";
    public Object data;
}
```

### Actions 动作
#### `ping`
data = null

#### `sendCode`
##### Request
data = uid (int)
##### Response
data = token (string)

#### `verify` 要求 `token`
##### Request
data = code (int)
##### Response
###### Success:
code = 200
###### Verification failed:
code = 400

#### `command` 要求 `token`
##### Request
data = command (string)
##### Response
data = message (string)
