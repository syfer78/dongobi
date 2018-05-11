package me.genomatch.storage.sharefile;

import me.genomatch.storage.FileInfo;
import me.genomatch.storage.FileStorage;
import me.genomatch.storage.sharefile.exception.*;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.BytesContentProvider;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

public class ShareFileStorage implements FileStorage {
    private static final Logger logger = Logger.getLogger(ShareFileStorage.class.getCanonicalName());
    private static final String ROOT_FOLDER_NAME = "GenoMatch.me";
    private static final String AUTH_URL = "https://diagnomics.sharefile.com/oauth/token";
    private static final String API_URL = "https://diagnomics.sharefile.com/sf/v3";

    private final String clientId;
    private final String clientSecret;
    private final String userId;
    private final String userPassword;

    private HttpClient httpClient;
    private String accessToken = null;
    private String refreshToken = null;
    private String rootFolderId;
    private long expiredTime;

    public ShareFileStorage(String clientId, String clientSecret, String userId, String userPassword) throws InitializeFailedException, LogInFailedException, UnknownException, RequestFailedException, ItemNotFoundException {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.userId = userId;
        this.userPassword = userPassword;

        httpClient = initHttpClient();
        try {
            httpClient.start();
        } catch (Exception e) {
            throw new InitializeFailedException(e);
        }

        login();

        String rootFolderInfo = getItemInfo("allshared", ROOT_FOLDER_NAME);

        JSONObject rootFolderInfoObj = new JSONObject(rootFolderInfo);
        rootFolderId = rootFolderInfoObj.getString("Id");
    }

    private HttpClient initHttpClient() {
        SslContextFactory sslContextFactory = new SslContextFactory();
        HttpClient httpClient = new HttpClient(sslContextFactory);
        httpClient.setFollowRedirects(false);

        return httpClient;
    }

    private void login() throws LogInFailedException {
        String content = "";
        try {
            if (refreshToken != null) {
                logger.info("[" + this.getClass().getSimpleName() + "]" + "Relogin to sharefile with Refresh Token : " + refreshToken);
                content = "grant_type=refresh_token&refresh_token=" + refreshToken + "&client_id=" + clientId + "&client_secret=" + clientSecret;
            } else {

                logger.info("[" + this.getClass().getSimpleName() + "]" + "Login to sharefile with id and password");
                content = "grant_type=password&client_id=" + clientId + "&client_secret=" + clientSecret + "&username=" + URLEncoder.encode(userId, "UTF-8") + "&password=" + URLEncoder.encode(userPassword, "UTF-8");
            }
        } catch (UnsupportedEncodingException e) {
            // Never Happened
        }

        ContentResponse response = null;
        try {
            response = httpClient.newRequest(AUTH_URL)
                                                   .method(HttpMethod.POST)
                                                   .content(new BytesContentProvider(content.getBytes("UTF-8")), "application/x-www-form-urlencoded")
                                                   .send();
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new LogInFailedException(e);
        } catch (UnsupportedEncodingException e) {
            // Never Happened
        }
        if(response == null) {
            throw new LogInFailedException("response is null");
        }
        int status = response.getStatus();

        logger.info("[" + this.getClass().getSimpleName() + "]" + "Login to sharefile with Status : " + status);
        if(status != 200) {
            throw new LogInFailedException(response.getContentAsString());
        }
        String responseBody = response.getContentAsString();
        JSONObject respJson = new JSONObject(responseBody);
        accessToken = respJson.getString("access_token");
        refreshToken = respJson.getString("refresh_token");
        expiredTime = System.currentTimeMillis() + respJson.getLong("expires_in")* 1000 - 1000;
        Calendar current = Calendar.getInstance();
        current.setTimeInMillis(expiredTime);
        logger.info("[" + this.getClass().getSimpleName() + "]" + "Token Refreshed. This will expired at : " + current.getTime());
    }

    private void checkExpired() throws LogInFailedException {
        if(System.currentTimeMillis() > expiredTime) {
            logger.info("[" + this.getClass().getSimpleName() + "]" + "Access Token expired. Re-Login");
            login();
        }
    }

    private String getItemInfo(String parentId, String itemPath) throws LogInFailedException, RequestFailedException, ItemNotFoundException, UnknownException {
        ContentResponse response;
        String url = API_URL + "/Items(" + parentId + ")/ByPath?path=/" + itemPath;
        checkExpired();
        try {
            response = httpClient.newRequest(url).method(HttpMethod.GET).header("Authorization", "Bearer " + accessToken).header("Content-Type", "application/json").send();
        } catch (InterruptedException | TimeoutException e) {
            throw new RequestFailedException(e);
        } catch (ExecutionException e) {
            login();
            return getItemInfo(parentId, itemPath);
        }
        int status = response.getStatus();
        switch(status) {
            case 401:
                login();
                return getItemInfo(parentId, itemPath);
            case 404:
                throw new ItemNotFoundException(itemPath + " Not Found");
            case 200:
                return response.getContentAsString();
            default:
                throw new UnknownException("API return " + status + " Code");
        }
    }

    private String createFolder(String parent, String folderName) throws RequestFailedException, LogInFailedException, UnknownException {
        String userHomeFolderCreationInfo = "{\"Name\":\"" + folderName + "\"}";
        ContentResponse response = null;
        checkExpired();
        try {
            response = httpClient.newRequest(API_URL + "/Items(" + parent + ")/Folder?overwrite=false")
                    .method(HttpMethod.POST)
                    .header("Authorization", "Bearer " + accessToken)
                    .content(new BytesContentProvider(userHomeFolderCreationInfo.getBytes("UTF-8")), "application/json")
                    .send();
        } catch (InterruptedException | TimeoutException | UnsupportedEncodingException e) {
            throw new RequestFailedException(e);
        } catch (ExecutionException e) {
            login();
            return createFolder(parent, folderName);
        }

        int status = response.getStatus();
        switch (status) {
            case 401:
                login();
                return createFolder(parent, folderName);
            case 409:
                try {
                    return getItemInfo(parent, folderName);
                } catch (ItemNotFoundException e) {
                    // Almost not possible @todo
                }
            case 200:
                return response.getContentAsString();
            default:
                throw new UnknownException("API return " + status + " Code");
        }
    }

    private String getDownloadPath(String itemId) throws RequestFailedException, LogInFailedException, ItemNotFoundException, UnknownException {
        ContentResponse response;
        String url = API_URL + "/Items(" + itemId + ")/Download";
        checkExpired();
        try {
            response = httpClient.newRequest(url).method(HttpMethod.GET).header("Authorization", "Bearer " + accessToken).send();
        } catch (InterruptedException | TimeoutException e) {
            throw new RequestFailedException(e);
        } catch (ExecutionException e) {
            login();
            return getDownloadPath(itemId);
        }
        int status = response.getStatus();
        switch(status) {
            case 401:
                login();
                return getDownloadPath(itemId);
            case 404:
                throw new ItemNotFoundException(itemId + " Not Found");
            case 302:
                return response.getHeaders().get("Location");
            default:
                throw new UnknownException("API return " + status + " Code");
        }
    }

    @Override
    public void createUserFolder(String userName) throws UnknownException, RequestFailedException, LogInFailedException {
        String homeFolderInfo = createFolder(rootFolderId, userName);
        JSONObject homeFolderInfoObj = new JSONObject(homeFolderInfo);
        String homeFolderId = homeFolderInfoObj.getString("Id");
        createFolder(homeFolderId, "data");
        createFolder(homeFolderId, "report");
    }

    @Override
    public void downloadFile(FileInfo fileInfo, OutputStream os) throws ItemNotFoundException, UnknownException, RequestFailedException, LogInFailedException {
        if(fileInfo == null) {
            throw new ItemNotFoundException("No File");
        }
        String itemId = fileInfo.getInfo("Id");
        String downloadPath = getDownloadPath(itemId);

        InputStreamResponseListener listener = new InputStreamResponseListener();
        httpClient.newRequest(downloadPath).send(listener);
        try (InputStream is = listener.getInputStream()){
            byte[] buffer = new byte[4096];
            int readBytes;
            while((readBytes = is.read(buffer))!= -1) {
                os.write(buffer, 0, readBytes);
            }
        } catch (IOException e) {
            throw new UnknownException(e);
        }
    }

    @Override
    public FileInfo getFileInfo(String srcFilePath) throws UnknownException, RequestFailedException, LogInFailedException, ItemNotFoundException {

        String itemInfo = getItemInfo(rootFolderId, srcFilePath);
        return new ShareFileFileInfo(itemInfo);

    }

    @Override
    public FileInfo getFileInfoFromShareId(String shareId) throws UnknownException, RequestFailedException, LogInFailedException {
        try {
            String itemInfo = getShareItemInfo(shareId);
            JSONObject shareItemInfo = new JSONObject(itemInfo);
            JSONArray array = shareItemInfo.getJSONArray("value");

            return new ShareFileFileInfo(array.getJSONObject(0));
        } catch (ItemNotFoundException e) {
            return null;
        }
    }

    private String getShareItemInfo(String shareId) throws RequestFailedException, LogInFailedException, ItemNotFoundException, UnknownException {
        ContentResponse response;
        String url = API_URL + "/Shares(" + shareId + ")/Items";
        checkExpired();
        try {
            response = httpClient.newRequest(url).method(HttpMethod.GET).header("Authorization", "Bearer " + accessToken).send();
        } catch (InterruptedException | TimeoutException e) {
            throw new RequestFailedException(e);
        } catch (ExecutionException e) {
            login();
            return getShareItemInfo(shareId);
        }
        int status = response.getStatus();
        switch(status) {
            case 401:
                login();
                return getShareItemInfo(shareId);
            case 404:
                throw new ItemNotFoundException(shareId + " Not Found");
            case 200:
                return response.getContentAsString();
            default:
                throw new UnknownException("API return " + status + " Code");
        }
    }
}
