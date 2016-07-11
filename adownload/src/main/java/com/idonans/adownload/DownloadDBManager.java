package com.idonans.adownload;

import com.idonans.acommon.internal.db.SimpleDB;

/**
 * download database
 * Created by pengji on 16-7-11.
 */
class DownloadDBManager {

    private static class InstanceHolder {

        private static final DownloadDBManager sInstance = new DownloadDBManager();

    }

    public static DownloadDBManager getInstance() {
        return InstanceHolder.sInstance;
    }

    private static final String TAG = "DownloadDBManager";
    private final SimpleDB mDownloadDB;
    private static final String KEY = "v1";

    private DownloadDBManager() {
        mDownloadDB = new SimpleDB("adownload");

        // 当后续结构有变更使得数据不兼容时，可以升级 KEY, 删除旧的内容.
        // 例如：

        // String jsonV2;
        // if (isFirstV2) {
        //     String jsonV1 = mDownloadDB.get("v1");
        //     jsonV2 = createFromV1(jsonV1);
        //     mDownloadDB.remove("v1");
        //     set(jsonV2);
        // } else {
        //     jsonV2 = get();
        // }
    }

    public String get() {
        return mDownloadDB.get(KEY);
    }

    public void set(String json) {
        mDownloadDB.set(KEY, json);
    }

    /**
     * for debug
     */
    public void printContent() {
        mDownloadDB.printAllRows();
    }

}
