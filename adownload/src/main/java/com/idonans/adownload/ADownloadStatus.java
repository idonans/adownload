package com.idonans.adownload;

/**
 * 下载状态
 * Created by pengji on 16-7-12.
 */
public interface ADownloadStatus {

    /**
     * 空闲状态. 排队等待下载
     */
    int STATUS_IDLE = 0;

    /**
     * 下载中
     */
    int STATUS_DOWNLOADING = 1;

    /**
     * 已暂停
     */
    int STATUS_PAUSED = 2;

    /**
     * 已停止
     */
    int STATUS_STOPED = 3;

    /**
     * 已完成
     */
    int STATUS_COMPLETE = 4;

    /**
     * 下载失败
     */
    int STATUS_ERROR = 5;

}
