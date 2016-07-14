package com.idonans.adownload;

/**
 * 下载状态
 * Created by pengji on 16-7-12.
 */
public class ADownloadStatus {

    /**
     * 空闲状态. 排队等待下载
     */
    public static final int STATUS_IDLE = 0;

    /**
     * 下载中
     */
    public static final int STATUS_DOWNLOADING = 1;

    /**
     * 已暂停
     */
    public static final int STATUS_PAUSED = 2;

    /**
     * 已停止
     */
    public static final int STATUS_STOPPED = 3;

    /**
     * 已完成
     */
    public static final int STATUS_COMPLETE = 4;

    /**
     * 下载失败
     */
    public static final int STATUS_ERROR = 5;

    public static String getStatus(int status) {
        switch (status) {
            case STATUS_COMPLETE:
                return "complete";
            case STATUS_DOWNLOADING:
                return "downloading";
            case STATUS_ERROR:
                return "error";
            case STATUS_IDLE:
                return "idle";
            case STATUS_PAUSED:
                return "paused";
            case STATUS_STOPPED:
                return "stopped";
            default:
                return "unknown";
        }
    }

}
