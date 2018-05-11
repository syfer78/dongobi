package me.genomatch.http.handler.service.user;


import me.genomatch.http.handler.service.BaseService;
import me.genomatch.storage.sharefile.exception.LogInFailedException;
import me.genomatch.storage.sharefile.exception.RequestFailedException;
import me.genomatch.storage.sharefile.exception.UnknownException;
import me.genomatch.util.ExceptionMessagemerger;
import sun.rmi.runtime.Log;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.logging.Logger;

public class UserDirCreateService extends BaseService {
    private static final Logger logger = Logger.getLogger(UserDirCreateService.class.getCanonicalName());
    public UserDirCreateService() throws Exception {
        super();
    }

    private boolean validateUserName(String userName) {
        try {
            return URLEncoder.encode(userName, "UTF-8").equals(userName);
        } catch (UnsupportedEncodingException e) {
            // Never Happened
            return false;
        }
    }

    @Override
    protected void onPost(HttpServletRequest req, HttpServletResponse resp) throws UnknownException {
        String url = req.getPathInfo();
        String userName = url.substring("/api/user/".length());
        if(!validateUserName(userName) ) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            logger.severe("[" + this.getClass().getSimpleName() + "]" + userName + " is not valid");
            return;
        }

        try {
            getStorage().createUserFolder(userName);
        } catch (UnknownException | RequestFailedException | LogInFailedException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            logger.severe("[" + this.getClass().getSimpleName() + "]" + ExceptionMessagemerger.mergeMessage(e));
        }
    }
}
