/*
 * gc-opencommand
 * Copyright (C) 2022  jie65535
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.github.jie65535.opencommand;

public class OpenCommandConfig {
    /**
     * 控制台 Token
     */
    public String consoleToken = "";

    /**
     * 验证码过期时间（单位秒）
     */
    public int codeExpirationTime_S = 60;

    /**
     * 临时Token过期时间（单位秒）
     */
    public int tempTokenExpirationTime_S = 300;

    /**
     * Token 最后使用过期时间（单位小时）
     */
    public int tokenLastUseExpirationTime_H = 48;

    /**
     * Socket 端口
     */
    public int socketPort = 5746;

    /**
     * Socket Token
     */
    public String socketToken = "";

    /**
     * Socket 主机地址
     */
    public String socketHost = "127.0.0.1";

    /**
     * Socket 显示名称
     */
    public String socketDisplayName = "";
}
