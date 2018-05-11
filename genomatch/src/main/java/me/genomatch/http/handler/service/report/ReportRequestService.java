package me.genomatch.http.handler.service.report;

import me.genomatch.http.handler.service.BaseService;
import me.genomatch.storage.FileInfo;
import me.genomatch.storage.sharefile.exception.LogInFailedException;
import me.genomatch.storage.sharefile.exception.RequestFailedException;
import me.genomatch.storage.sharefile.exception.UnknownException;
import me.genomatch.util.ExceptionMessagemerger;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.BytesContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class ReportRequestService extends BaseService {
    private static final Logger logger = Logger.getLogger(ReportRequestService.class.getCanonicalName());
    private static final String COLO_URL = "http://192.175.54.248:9259/report";
    private HttpClient httpClient;
    public ReportRequestService() throws Exception {
        super();
        SslContextFactory sslContextFactory = new SslContextFactory();
        httpClient = new HttpClient(sslContextFactory);
        httpClient.setFollowRedirects(false);
        httpClient.start();
    }

    @Override
    protected void onPost(HttpServletRequest req, HttpServletResponse resp) throws UnknownException {
        String filePath = req.getPathInfo().substring("/api/report/".length());
        if(filePath.endsWith("/")) {
            filePath = filePath.substring(0, filePath.length()-1);
        }
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(req.getInputStream()));
            String line = "";
            StringBuilder body = new StringBuilder();
            while ((line = br.readLine()) != null) {
                body = body.append(line).append("\n");
            }
            logger.info("[" + this.getClass().getSimpleName() + "]" + "Request : " + body);
            sendAsyncRequest(filePath, body.toString());
            resp.setStatus(HttpServletResponse.SC_OK);
            logger.info("[" + this.getClass().getSimpleName() + "]" + "Request Finished.");
        } catch (IOException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            logger.severe("[" + this.getClass().getSimpleName() + "]" + ExceptionMessagemerger.mergeMessage(e) );
        }
    }

    private void sendAsyncRequest(final String orderId, String body) {
        final Map<String, String> parsed = parseUrlForm(body);
        try {
            httpClient.newRequest(COLO_URL)
                    .method(HttpMethod.POST)
                    .content(new BytesContentProvider(body.getBytes("UTF-8")), "application/x-www-form-urlencoded")
                    .send(new Response.Listener.Adapter() {
                        @Override
                        public void onContent(Response response, ByteBuffer content) {
                            if (response.getStatus() == HttpServletResponse.SC_OK) {
                                doDBUpdate(orderId, parsed, content);
                            } else {
                                logger.severe("[" + this.getClass().getSimpleName() + "]" + "Error occured : " + response.getReason());
                            }
                        }

                        @Override
                        public void onFailure(Response response, Throwable failure) {
                            logger.severe("[" + this.getClass().getSimpleName() + "]" + "Failed : " + failure.getMessage());
                        }
                    });
        } catch (UnsupportedEncodingException e) {
            // Never Happened
        }
    }

    private void doDBUpdate(String orderId, Map<String, String> parsed, ByteBuffer contents) {
        byte[] bytes = new byte[contents.remaining()];
        contents.get(bytes);
        String body = new String(bytes);
        // Body : {"report": "https://diagnomics.sharefile.com/d-s040a486ffe149cab"}
        JSONObject bodyObj = new JSONObject(body);
        String reportUrl = bodyObj.getString("report");
        String shareId = reportUrl.substring(reportUrl.lastIndexOf('/')+3); // remove 'd-'
        Connection con = null;
        PreparedStatement ps = null;
        try {
            FileInfo fileInfo = getStorage().getFileInfoFromShareId(shareId);
            String url = "/" + parsed.get("user_id") + "/" + "report/" + fileInfo.getInfo("FileName");

            con = getConnection();

            ps = con.prepareStatement("Update wp_genomatchme_order set status=?, data_url=? where id=?");
            ps.setString(1, "COMPLETED");
            ps.setString(2, url);
            ps.setString(3, orderId);

            ps.executeUpdate();
        } catch (UnknownException | RequestFailedException | LogInFailedException | SQLException e) {
            logger.severe("[" + this.getClass().getSimpleName() + "]" + ExceptionMessagemerger.mergeMessage(e));
        } finally {
            if(con != null) {
                try {

                    con.close();
                } catch (SQLException e) {
                    logger.severe("[" + this.getClass().getSimpleName() + "]" + ExceptionMessagemerger.mergeMessage(e));
                }
            }
            if(ps != null) {
                try {
                    ps.close();
                } catch (SQLException e) {
                    logger.severe("[" + this.getClass().getSimpleName() + "]" + ExceptionMessagemerger.mergeMessage(e));
                }
            }
        }
    }

    private Map<String, String> parseUrlForm(String body) {
        Map<String, String> parsedMap = new HashMap<String, String>();
        String[] parsed = body.split("&");
        for(String parsedParam:parsed) {
            String[] parsedKV = parsedParam.split("=");
            try {
                parsedMap.put(parsedKV[0].trim(), URLDecoder.decode(parsedKV[1].trim(), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                // Never Happened
            }
        }

        return parsedMap;
    }
}
