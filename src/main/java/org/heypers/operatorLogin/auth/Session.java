package org.heypers.operatorLogin.auth;

import java.util.UUID;

public class Session {
    private UUID PlayerUUID;
    private Long LoginTime;

    public Session(UUID PlayerUUID, Long LoginTime){
        this.PlayerUUID = PlayerUUID;
        this.LoginTime = LoginTime;
    }

    public UUID getPlayerUUID(){
        return PlayerUUID;
    }

    public Long getLoginTime(){
        return LoginTime;
    }
}
