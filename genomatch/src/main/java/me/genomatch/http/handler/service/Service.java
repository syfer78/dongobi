package me.genomatch.http.handler.service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface Service {
    void onService(HttpServletRequest req, HttpServletResponse resp) throws Exception;
}
