package com.idonans.adownload;

import android.text.TextUtils;

import com.idonans.acommon.internal.db.SimpleDB;
import com.idonans.acommon.lang.CommonLog;

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

    private static final String KEY_TIME_FIRST_START = "time_first_start";
    private final long mTimeFirstStart;

    private static final String KEY_TIME_START = "time_start";
    private final long mTimeStart;

    private static final String KEY_JSON = "adownload_json_1";

    private ADownloadDBManager() {
        mDownloadDB = new SimpleDB("adownload");

        long timeNow = System.currentTimeMillis();

        long timeFirstStart = valueOf(mDownloadDB.get(KEY_TIME_FIRST_START), 0L);
        long timeStart = valueOf(mDownloadDB.get(KEY_TIME_START), 0L);

        if (timeFirstStart <= 0 || timeStart <= 0) {
            CommonLog.d(TAG + " first use or data format error, clear database");
            mDownloadDB.clear();

            timeFirstStart = timeNow;
            timeStart = timeNow;
        } else {
            timeStart = timeNow;
        }

        mTimeFirstStart = timeFirstStart;
        mTimeStart = timeStart;
        mDownloadDB.set(KEY_TIME_FIRST_START, String.valueOf(mTimeFirstStart));
        mDownloadDB.set(KEY_TIME_START, String.valueOf(mTimeStart));
    }

    public String getContent() {
        return mDownloadDB.get(KEY_JSON);
    }

    public void setContent(String json) {
        mDownloadDB.set(KEY_JSON, json);
    }

    private static long valueOf(String source, long defaultValue) {
        try {
            if (TextUtils.isEmpty(source)) {
                return defaultValue;
            }
            return Long.parseLong(source);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return defaultValue;
    }

    public long getTimeFirstStart() {
        return mTimeFirstStart;
    }

    public long getTimeStart() {
        return mTimeStart;
    }

    /**
     * for debug
     */
    public void printContent() {
        mDownloadDB.printAllRows();
    }

}
