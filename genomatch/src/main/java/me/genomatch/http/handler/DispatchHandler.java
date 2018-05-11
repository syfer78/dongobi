package me.genomatch.http.handler;

import me.genomatch.http.handler.service.file.FileManagementService;
import me.genomatch.http.handler.service.report.ReportRequestService;
import me.genomatch.http.handler.service.Service;
import me.genomatch.http.handler.service.user.UserDirCreateService;
import me.genomatch.util.ExceptionMessagemerger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.DefaultHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;

public class DispatchHandler extends DefaultHandler {
    private static final Logger logger = Logger.getLogger(DispatchHandler.class.getCanonicalName());
    private Service userService;
    private Service fileService;
    private Service reportService;

    public DispatchHandler() throws Exception {
        userService = new UserDirCreateService();
        fileService = new FileManagementService();
        reportService = new ReportRequestService();
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        logger.info("[" + this.getClass().getSimpleName() + "]" + "Request Path : " + target);
        if(!target.startsWith("/api")) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
        try {
            if (target.startsWith("/api/user")) {
                userService.onService(request, response);
            } else if (target.startsWith("/api/file")) {
                fileService.onService(request, response);
            } else if (target.startsWith("/api/report")) {
                reportService.onService(request, response);
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            logger.severe("[" + this.getClass().getSimpleName() + "]" + ExceptionMessagemerger.mergeMessage(e));
        }
        baseRequest.setHandled(true);
    }
}
