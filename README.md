# gc-opencommand-plugin

中文 | [English](README_en-US.md)

一个为第三方客户端开放GC命令执行接口的插件

自 `1.7.0` 起可以通过 `|` 或者换行来分隔多条命令，例如：
```shell
/a 1 | /a 2
/a 3
```

调用 `ping` 响应数据将包含插件版本号。

## 使用本插件的应用
- [GrasscutterTools](https://github.com/jie65535/GrasscutterCommandGenerator) —— Windows 客户端工具
- [JGrasscutterCommand](https://github.com/jie65535/JGrasscutterCommand) —— [Mirai](https://github.com/mamoe/mirai) 插件，在QQ里执行命令
- [Yunzai-GrasscutterCommand](https://github.com/Zyy-boop/Yunzai-GrasscutterCommand) —— Yunzai-bot插件，在QQ里执行命令
- 待补充

## 服务端安装

1. 在 [Release](https://github.com/jie65535/gc-opencommand-plugin/releases) 下载 `jar`
2. 放入 `grasscutter/plugins` 文件夹
3. 重启 `grasscutter` 即可生效

## 玩家使用流程

1. 在远程工具中填写服务地址，查询插件状态
2. 填写UID，发送验证码（需要在线）
3. 将游戏内收到的**4位整数验证码**填入工具校验
4. 享受便利！

## 控制台连接

1. 首次启动时，会在 `plugins` 目录下生成一个 `opencommand-plugin` 目录，打开并编辑 `config.json`
2. 设置 `consoleToken` 的值为你的连接秘钥，建议使用至少32字符的长随机字符串。（检测到为空时会自动生成，生成时会在控制台中输出）
3. 重新启动服务端即可生效配置
4. 在工具中选择控制台身份，并填写你的 `consoleToken` 即可以控制台身份运行指令

---

## 客户端请求流程

1. `ping` 确认是否支持 `opencommand` 插件
2. `sendCode` 向指定玩家发送验证码（1分钟内不允许重发），保存返回的 `token`
3. 使用 `token` 和**4位整数验证码**发送 `verify` 校验
4. 如果验证通过，可以使用该 `token` 执行 `command` 动作

## 插件构建说明

1. 克隆仓库
2. 在目录下新建 `lib` 目录
3. 将 `grasscutter.jar` 放入 `lib` 目录
4. 执行 `gradle build`

## 多服务器
### 主服务器 (Dispatch)
1. 在 `opencommand-plugin` 目录下打开 `config.json`
2. 修改 `socketPort` 值为一个未被使用的端口
3. 设置 `socketToken` 多服务器通信密钥，建议使用至少32字符的长随机字符串。
4. 重新启动服务端即可生效配置

### 子服务器 (Game)
1. 在 `opencommand-plugin` 目录下打开 `config.json`
2. 修改 `socketHost` 和 `socketPort` 值为主服务器的地址和端口
3. 设置 `socketToken` 和主服务器相同的值
4. 设置 `socketDisplayName` 值为你的服务器名称 (用途请见[下方](https://github.com/jie65535/gc-opencommand-plugin#%E8%8E%B7%E5%8F%96%E5%A4%9A%E6%9C%8D%E5%8A%A1%E5%99%A8%E5%88%97%E8%A1%A8))
5. 重新启动服务端即可生效配置

---

## `config.json`

```json5
{
  // 控制台连接令牌（检测到空时会自动生成）
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
  // 多服务器Dispatch服务器地址
  "socketHost": "127.0.0.1",
  // 多服务器显示名称
  "socketDisplayName": ""
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
    public String server = "";
    public Object data = null;
}
```

### Response 响应

```java
public final class JsonResponse {
    public int retcode = 200;
    public String message = "Success";
    public Object data;
}
```

### Actions 动作

#### `测试连接`

##### Request

| 请求参数   | 请求数据   | 类型       |
|--------|--------|----------|
| action | `ping` | `String` |

##### Response

| 返回参数    | 返回数据      | 类型       |
|---------|-----------|----------|
| retcode | `200`     | `Int`    |
| message | `Success` | `String` |
| data    | `null`    | `null`   |

#### `获取在线玩家`

##### Request

| 请求参数   | 请求数据     | 类型       |
|--------|----------|----------|
| action | `online` | `String` |

##### Response

| 返回参数    | 返回数据                            | 类型           |
|---------|---------------------------------|--------------|
| retcode | `200`                           | `Int`        |
| message | `Success`                       | `String`     |
| data    | `{"count": 0, playerList": []}` | `JsonObject` |

#### `发送验证码`

##### Request

| 请求参数   | 请求数据       | 类型       |
|--------|------------|----------|
| action | `sendCode` | `String` |
| data   | `uid`      | `Int`    |

##### Response

| 返回参数    | 返回数据      | 类型       |
|---------|-----------|----------|
| retcode | `200`     | `Int`    |
| message | `Success` | `String` |
| data    | `token`   | `String` |

#### `验证验证码`

##### Request

| 请求参数   | 请求数据     | 类型       |
|--------|----------|----------|
| action | `verify` | `String` |
| token  | `token`  | `String` |
| data   | `code`   | `Int`    |

##### Response

成功

| 返回参数    | 返回数据      | 类型       |
|---------|-----------|----------|
| retcode | `200`     | `Int`    |
| message | `Success` | `String` |
| data    | `null`    | `null`   |

失败

| 返回参数    | 返回数据                  | 类型       |
|---------|-----------------------|----------|
| retcode | `400`                 | `Int`    |
| message | `Verification failed` | `String` |
| data    | `null`                | `null`   |

#### `执行命令`

##### Request

| 请求参数   | 请求数据      | 类型       |
|--------|-----------|----------|
| action | `command` | `String` |
| token  | `token`   | `String` |
| data   | `command` | `String` |

##### Response

成功

| 返回参数    | 返回数据             | 类型       |
|---------|------------------|----------|
| retcode | `200`            | `Int`    |
| message | `Success`        | `String` |
| data    | `Command return` | `String` |

### 执行控制台命令

#### `获取运行模式`

##### Request

| 请求参数   | 请求数据      | 类型       |
|--------|-----------|----------|
| action | `runmode` | `String` |
| token  | `token`   | `String` |

##### Response

成功

| 返回参数    | 返回数据                  | 类型       |
|---------|-----------------------|----------|
| retcode | `200`                 | `Int`    |
| message | `Success`             | `String` |
| data    | `1 (多服务器) / 0 (单服务器)` | `Int`    |

#### `获取多服务器列表`

##### Request

| 请求参数   | 请求数据     | 类型       |
|--------|----------|----------|
| action | `server` | `String` |
| token  | `token`  | `String` |

##### Response

成功

| 返回参数    | 返回数据      | 类型           |
|---------|-----------|--------------|
| retcode | `200`     | `Int`        |
| message | `Success` | `String`     |
| data    | `{}`      | `JsonObject` |

```json5
{
  "retcode": 200,
  "message": "success",
  "data": {
    // 服务器 UUID
    "13d82d0d-c7d9-47dd-830c-76588006ef6e": "2.8.0 服务器",
    "e6b83224-a761-4023-be57-e054c5bb823a": "2.8.0 开发服务器"
  }
}
```

#### `执行命令`

##### Request

> 如果为单服务器则无需填写服务器 UUID

| 请求参数   | 请求数据      | 类型       |
|--------|-----------|----------|
| action | `command` | `String` |
| token  | `token`   | `String` |
| server | `UUID`    | `String` |
| data   | `command` | `String` |

##### Response

成功

| 返回参数    | 返回数据             | 类型       |
|---------|------------------|----------|
| retcode | `200`            | `Int`    |
| message | `Success`        | `String` |
| data    | `Command return` | `String` |
