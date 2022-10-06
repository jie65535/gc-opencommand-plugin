package com.github.jie65535.opencommand;

import com.github.jie65535.opencommand.model.Client;

import java.util.Date;
import java.util.Vector;

/**
 * 插件持久化数据
 */
public class OpenCommandData {

    /**
     * 连接的客户端列表
     */
    public Vector<Client> clients = new Vector<>();

    /**
     * 通过令牌获取客户端
     * @param token 令牌
     * @return 客户端对象，若未找到返回null
     */
    public Client getClientByToken(String token) {
        for (var c : clients) {
            if (c.token.equals(token))
                return c;
        }
        return null;
    }

    /**
     * 移除所有过期的客户端
     */
    public void removeExpiredClients() {
        var now = new Date();
        clients.removeIf(client -> client.tokenExpireTime.before(now));
    }

    public void addClient(Client client) {
        clients.add(client);
    }
}
