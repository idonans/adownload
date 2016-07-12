package com.idonans.adownload;

/**
 * Created by pengji on 16-7-12.
 */
public class ADownloadTask {


    /**
     * 下载任务 id，基于 httpUrl 计算得到，相同的资源具有相同的 id
     */
    public String id;

    /**
     * 资源地址, http 格式
     */
    public String httpUrl;

    /**
     * 资源本地保存路径
     */
    public String localPath;

    /**
     * 资源长度, 可能 < 0
     */
    public long contentLength;

    /**
     * 资源已经下载的长度, 总是 >= 0
     */
    public long downloadLength;

    /**
     * 参考 http 协议，断点续传时用来辅助校验资源是否一致
     */
    public long lastModify;

    /**
     * 该资源是否支持断点续传
     */
    public boolean canContinue;

    /**
     * 下载状态
     */
    public int status = ADownloadStatus.STATUS_IDLE;

    public void onCreate() {
        // 恢复下载状态
        // 下载中的，空闲的和其它未知状态的任务一律调整为空闲状态(排队等待下载)
        switch (this.status) {
            case ADownloadStatus.STATUS_COMPLETE:
            case ADownloadStatus.STATUS_ERROR:
            case ADownloadStatus.STATUS_STOPED:
            case ADownloadStatus.STATUS_PAUSED:
                break;
            case ADownloadStatus.STATUS_DOWNLOADING:
            case ADownloadStatus.STATUS_IDLE:
            default:
                this.status = ADownloadStatus.STATUS_IDLE;
                break;
        }
    }

}
