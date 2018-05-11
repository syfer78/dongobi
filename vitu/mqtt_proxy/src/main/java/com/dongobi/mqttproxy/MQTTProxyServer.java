package com.dongobi.mqttproxy;

import com.dongobi.mqttproxy.filter.MessageFilter;

public class MQTTProxyServer {
    private final int port;

    public MQTTProxyServer(int port) {
        this.port = port;
    }

    public void addFilter(MessageFilter filter) {
        
    }
}
