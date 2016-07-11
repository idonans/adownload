package com.idonans.adownload;

/**
 * Android Download Manager singleton
 * Created by pengji on 16-7-11.
 */
public class ADownloadManager {

    private static class InstanceHolder {

        private static final ADownloadManager sInstance = new ADownloadManager();

    }

    public static ADownloadManager getInstance() {
        return InstanceHolder.sInstance;
    }

    private ADownloadManager() {
    }

}
