# gc-opencommand-plugin

[中文](README.md) | English

A plugin that opens the GC command execution interface for third-party clients

## Server installation
1. Download the `jar` in [Release](https://github.com/jie65535/gc-opencommand-plugin/releases)
2. Put it in the `plugins` folder

## Console connection
1. When starting for the first time, a `opencommand-plugin` directory will be generated under the `plugins` directory, open and edit `config.json`
2. Set the value of `consoleToken` to your connection key. It is recommended to use a long random string of at least 32 characters.
3. Restart the server to take effect
4. Select the console identity in the client, and fill in your `consoleToken` to run the command as the console identity

## Build
1. `git clone https://github.com/jie65535/gc-opencommand-plugin`
2. `cd gc-opencommand-plugin`
3. `mkdir lib`
4. `mv path/to/grasscutter-1.x.x-dev.jar ./lib`
5. `gradle build`

## Player
1. Fill in the service address in the client to confirm whether it supports
2. Fill in the UID and send the verification code
3. Fill in the **4-digit integer verification code** received in the game into the client verification
4. Enjoy the convenience!

## Client request
1. `ping` to confirm whether the `opencommand` plugin is supported
2. `sendCode` sends a verification code to the specified player (re-send is not allowed within 1 minute), and save the returned `token`
3. Send `verify` check using `token` and **4-digit integer verification code**
4. If the verification is passed, you can use the `token` to execute the `command` action

---

## `config.json`
```json
{
  "consoleToken": "",
  "codeExpirationTime_S": 60,
  "tempTokenExpirationTime_S": 300,
  "tokenLastUseExpirationTime_H": 48,
  "socketPort": 5746,
  "socketToken": "",
  "socketHost": "127.0.0.1"
}
```


## API `/opencommand/api`
Example
```
https://127.0.0.1/opencommand/api
```

## Request
```java
public final class JsonRequest {
    public String token = "";
    public String action = "";
    public Object data = null;
}
```

## Response
```java
public final class JsonResponse {
    public int retcode = 200;
    public String message = "success";
    public Object data;
}
```

### Actions 
#### `Test connect`

##### Request

| Request |  Request data  |  type  |
| ------- | -------------- | ------ |
| action  | `ping`         |`String`|

##### Response

| Response |  Response data  |  type  |
| -------- | --------------- | ------ |
| retcode  | `200`           |`String`|
| message  | `success`       |`String`|
| data     | `null`          |`null`  |

#### `Send code`

##### Request

| Request |  Request data  |  type  |
| ------- | -------------- | ------ |
| action  | `sendCode`     |`String`|
| data    | `uid`          |`Int`   |

##### Response

| Response |  Response data  |  type  |
| -------- | --------------- | ------ |
| retcode  | `200`           |`String`|
| message  | `success`       |`String`|
| data     | `token`         |`String`|

#### `Verify code`

##### Request

| Request |  Request data  |  type  |
| ------- | -------------- | ------ |
| action  | `verify`       |`String`|
| token   | `token`        |`String`|
| data    | `code`         |`Int`   |

##### Response

Success

| Response |  Response data  |  type  |
| -------- | -------------- | ------ |
| retcode  | `200`          |`String`|
| message  | `success`      |`String`|
| data     | `null`         | `null` |

Failed

| Response |    Response data    |  type  |
| -------- | -------------------- | ------ |
| retcode  | `400`                |`String`|
| message  | `Verification failed`|`String`|
| data     | `null`               |`null`  |

#### `Run command`

##### Request

| Request |  Request data  |  type  |
| ------- | -------------- | ------ |
| action  | `command`      |`String`|
| token   | `token`        |`String`|
| data    | `command`      |`String`|

##### Response

Success

| Response |   Response data   |  type  |
| -------- | ------------------ | ------ |
| retcode  | `200`              |`String`|
| message  | `success`          |`String`|
| data     | `Command return`   |`String`|