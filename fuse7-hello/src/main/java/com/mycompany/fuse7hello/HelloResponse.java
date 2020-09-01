package com.mycompany.fuse7hello;

import java.net.InetAddress;

public class HelloResponse {
    private String message;

    public HelloResponse(String message) {
        this.message = message;
    }

    public String getMessage() throws Exception {
        return message + " - " + InetAddress.getLocalHost().getHostName();
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
