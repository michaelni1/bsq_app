package com.bsq.bsquared;

import java.util.Date;

public class Chat {

    String sender, message;
    Boolean is_host = false;
    Date timestamp;

    public Chat(String sender, String message, Boolean is_host, Date timestamp) {
        this.sender = sender;
        this.message = message;
        this.is_host = is_host;
        this.timestamp = timestamp;
    }

    public Chat(){}

    public void setSender(String sender) {
        this.sender = sender;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public void setIs_host(Boolean is_host) { this.is_host = is_host; }

    public String getSender() {
        return sender;
    }

    public String getMessage() {
        return message;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public Boolean getIs_host() { return is_host; }
}

