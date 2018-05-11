package com.dongobi.mqttproxy.filter;

import com.dongobi.mqttproxy.message.MQTTMessage;

public interface MessageFilter {
    void init(MessageFilterConfig fc);
    void doFilter(MQTTMessage message);
    void destroy();
}
