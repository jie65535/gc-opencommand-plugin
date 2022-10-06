package com.github.jie65535.opencommand.model;

import java.util.Date;

public final class Client {

    public String token;
    public Integer playerId;
    public Date tokenExpireTime;

    public Client(String token, Integer playerId, Date tokenExpireTime) {
        this.token = token;
        this.playerId = playerId;
        this.tokenExpireTime = tokenExpireTime;
    }
}
