package me.genomatch.http.handler.service.file;

import me.genomatch.http.handler.service.BaseService;
import me.genomatch.storage.FileInfo;
import me.genomatch.storage.sharefile.exception.ItemNotFoundException;
import me.genomatch.storage.sharefile.exception.LogInFailedException;
import me.genomatch.storage.sharefile.exception.RequestFailedException;
import me.genomatch.storage.sharefile.exception.UnknownException;
import me.genomatch.util.ExceptionMessagemerger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class FileManagementService extends BaseService {
    private static final Logger logger = Logger.getLogger(FileManagementService.class.getCanonicalName());
    public FileManagementService() throws Exception {
        super();
    }

    @Override
    protected void onGet(HttpServletRequest req, HttpServletResponse resp) throws UnknownException {
        String sessionId = req.getParameter("session_id");
        try {
            if(!validateSessionId(sessionId)) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                logger.severe("[" + this.getClass().getSimpleName() + "]" + "Session Id [" + sessionId + "] is invalid.");
                return;
            }
        } catch (SQLException e) {
            logger.severe("[" + this.getClass().getSimpleName() + "]" + ExceptionMessagemerger.mergeMessage(e));
            throw new UnknownException(e);
        }
        String filePath = req.getPathInfo().substring("/api/file/".length());

        try {
            FileInfo fileInfo = getStorage().getFileInfo(filePath);
            resp.setStatus(HttpServletResponse.SC_OK);
            String fileName = fileInfo.getInfo("FileName");
            String fileSize = fileInfo.getInfo("FileSizeBytes");
            resp.addHeader("Content-Disposition", "attachment;filename=\"" + fileName + "\";");
            resp.setContentType("application/pdf");
            resp.addHeader("Content-Length", fileSize);

            getStorage().downloadFile(fileInfo, resp.getOutputStream());
        } catch (IOException | RequestFailedException | LogInFailedException e) {
            logger.severe("[" + this.getClass().getSimpleName() + "]" + ExceptionMessagemerger.mergeMessage(e));
            throw new UnknownException(e);
        } catch (ItemNotFoundException e) {
            logger.severe("[" + this.getClass().getSimpleName() + "]" + ExceptionMessagemerger.mergeMessage(e));
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void onPost(HttpServletRequest req, HttpServletResponse resp) throws UnknownException {
        String filePath = req.getPathInfo().substring("/api/file".length());
        if(!filePath.endsWith("/")) {
            filePath += "/";
        }
        Connection con = null;
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(req.getInputStream()));
            String line = "";
            StringBuilder body = new StringBuilder();
            while((line=br.readLine()) != null) {
                body = body.append(line).append("\n");
            }

            logger.info("[" + this.getClass().getSimpleName() + "]" + "Request Body : " + body);
            Map<String, String> params = parseUrlForm(body.toString());
            String testKitId = params.get("testkit_id");
            String dataUrl = URLDecoder.decode(params.get("data_url"), "UTF-8");

            String shareId = dataUrl.substring(dataUrl.lastIndexOf('/')+3); // remove 'd-'
            FileInfo fileInfo = getStorage().getFileInfoFromShareId(shareId);
            String url = filePath + fileInfo.getInfo("FileName");
            logger.info("[" + this.getClass().getSimpleName() + "]" + "Download Url : " + url);

            con = getConnection();
            PreparedStatement ps = con.prepareStatement("Update wp_genomatchme_testkit set status=?, data_url=?, filename=? where id=?");
            ps.setString(1, "CONFIRMED");
            ps.setString(2, url);
            ps.setString(3, fileInfo.getInfo("FileName"));
            ps.setString(4, testKitId);

            ps.executeUpdate();
            logger.info("[" + this.getClass().getSimpleName() + "]" + "Update Complete");
            ps.close();
        } catch (SQLException | RequestFailedException | LogInFailedException | IOException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            logger.severe("[" + this.getClass().getSimpleName() + "]" + ExceptionMessagemerger.mergeMessage(e));
            return;
        } finally {
            if(con != null) {
                try {
                    con.close();
                } catch (SQLException e) {
                    logger.severe("[" + this.getClass().getSimpleName() + "]" + ExceptionMessagemerger.mergeMessage(e));
                }
            }
        }
        resp.setStatus(HttpServletResponse.SC_OK);
        logger.info("[" + this.getClass().getSimpleName() + "]" + "Return OK");
    }

    private boolean validateSessionId(String sessionId) throws SQLException {
        if(sessionId == null || sessionId.isEmpty()) {
            return false;
        }
        Connection con = null;
        PreparedStatement ps = null;
        PreparedStatement removePs = null;
        ResultSet rs = null;
        try {
            con = getConnection();

            ps = con.prepareStatement("Select * from wp_genomatchme_download where user_login=?");

            ps.setString(1, sessionId);
            rs = ps.executeQuery();
            removePs= con.prepareStatement("Delete from wp_genomatchme_download where user_login=?");

            if(rs.next()) {
                removePs.setString(1, sessionId);
                removePs.executeUpdate();
                return true;
            } else {
                return false;
            }
        } finally {
            if(removePs != null) {
                removePs.close();
            }
            if(rs != null) {
                rs.close();
            }
            if(ps != null) {
                ps.close();
            }
            if(con != null) {
                con.close();
            }
        }
    }

    private Map<String, String> parseUrlForm(String body) {
        Map<String, String> parsedMap = new HashMap<String, String>();
        String[] parsed = body.split("&");
        for(String parsedParam:parsed) {
            String[] parsedKV = parsedParam.split("=");
            parsedMap.put(parsedKV[0].trim(), parsedKV[1].trim());
        }

        return parsedMap;
    }
}
