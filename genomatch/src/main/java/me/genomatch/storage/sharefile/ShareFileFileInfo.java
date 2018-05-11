package me.genomatch.storage.sharefile;

import me.genomatch.storage.FileInfo;
import org.json.JSONObject;

public class ShareFileFileInfo implements FileInfo {
    private JSONObject object;

    public ShareFileFileInfo(String itemInfo) {
        object = new JSONObject(itemInfo);
    }

    public ShareFileFileInfo(JSONObject jsonObject) {
        object = jsonObject;
    }

    public String getInfo(String key) {
        return object.getString(key);
    }
}
