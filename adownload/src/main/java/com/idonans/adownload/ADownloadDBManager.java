package com.idonans.adownload;

import com.idonans.acommon.internal.db.SimpleDB;

/**
 * download database
 * Created by pengji on 16-7-11.
 */
class ADownloadDBManager {

    private static class InstanceHolder {

        private static final ADownloadDBManager sInstance = new ADownloadDBManager();

    }

    public static ADownloadDBManager getInstance() {
        return InstanceHolder.sInstance;
    }

    private static final String TAG = "ADownloadDBManager";
    private final SimpleDB mDownloadDB;
    private static final String KEY = "c_download";

    private ADownloadDBManager() {
        mDownloadDB = new SimpleDB("adownload");
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
