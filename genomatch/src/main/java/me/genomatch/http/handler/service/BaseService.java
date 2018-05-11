package me.genomatch.http.handler.service;

import me.genomatch.database.DatabasePool;
import me.genomatch.storage.FileStorage;
import me.genomatch.storage.sharefile.ShareFileStorage;
import me.genomatch.storage.sharefile.exception.UnknownException;
import me.genomatch.util.ExceptionMessagemerger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;

public class BaseService implements Service {
    private static final Logger logger = Logger.getLogger(BaseService.class.getCanonicalName());
    private static FileStorage storage = null;
    private static DatabasePool dbPool = null;
    protected BaseService() throws Exception {
        Properties prop = new Properties();
        prop.load(ClassLoader.getSystemResourceAsStream("me/genomatch/resources.properties"));

        if(storage == null) {
            storage = new ShareFileStorage(prop.getProperty("storage_client_id"), prop.getProperty("storage_client_secret"), prop.getProperty("storage_user_id"), prop.getProperty("storage_user_password"));
        }
        if(dbPool == null) {
            dbPool = new DatabasePool(prop.getProperty("db_type"), prop.getProperty("db_name"), prop.getProperty("db_user_name"), prop.getProperty("db_user_password"), prop.getProperty("db_host_name"), prop.getProperty("db_port"));
        }
    }

    protected Connection getConnection() throws SQLException {
        return dbPool.getConnection();
    }

    protected FileStorage getStorage() {
        return storage;
    }

    @Override
    public void onService(HttpServletRequest req, HttpServletResponse resp) throws UnknownException {
        try {
            switch (req.getMethod()) {
                case "POST":
                    onPost(req, resp);
                    break;
                case "GET":
                    onGet(req, resp);
                    break;
                case "PUT":
                    onPut(req, resp);
                    break;
                default:
                    resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            }
        } catch (UnknownException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            logger.severe("[" + this.getClass().getSimpleName() + "]" + ExceptionMessagemerger.mergeMessage(e));
        }
    }

    protected void onPut(HttpServletRequest req, HttpServletResponse resp) throws UnknownException {
        resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
    }
    protected void onGet(HttpServletRequest req, HttpServletResponse resp) throws UnknownException {
        resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
    }
    protected void onPost(HttpServletRequest req, HttpServletResponse resp) throws UnknownException {
        resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
    }
}
