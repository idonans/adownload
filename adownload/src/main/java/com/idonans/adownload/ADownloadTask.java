package com.idonans.adownload;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.idonans.acommon.lang.Progress;
import com.idonans.acommon.util.FileUtil;

import java.io.File;

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

    private transient ADownloadEngine.DownloadRunner mDownloadRunner;

    public ADownloadEngine.DownloadRunner getDownloadRunner() {
        return mDownloadRunner;
    }

    public void setDownloadRunner(ADownloadEngine.DownloadRunner downloadRunner) {
        mDownloadRunner = downloadRunner;
    }

    public void onCreate() {
        // 恢复下载状态
        // 下载中的，空闲的和其它未知状态的任务一律调整为空闲状态(排队等待下载)
        switch (this.status) {
            case ADownloadStatus.STATUS_COMPLETE:
            case ADownloadStatus.STATUS_ERROR:
            case ADownloadStatus.STATUS_STOPPED:
            case ADownloadStatus.STATUS_PAUSED:
                break;
            case ADownloadStatus.STATUS_DOWNLOADING:
            case ADownloadStatus.STATUS_IDLE:
            default:
                this.status = ADownloadStatus.STATUS_IDLE;
                break;
        }

        // 如果下载文件缺失，重新创建一个
        if (!TextUtils.isEmpty(this.localPath)) {
            File targetFile = new File(localPath);
            if (!targetFile.exists()) {
                FileUtil.createNewFileQuietly(targetFile);
            }
        }
    }

    public Snapshot getSnapshot() {
        return new Snapshot(this);
    }

    @NonNull
    static ADownloadTask create(@NonNull ADownloadRequest request) {
        ADownloadTask task = new ADownloadTask();
        task.id = request.getId();
        task.httpUrl = request.getHttpUrl();

        String localPath = request.getLocalPath();
        task.localPath = AUtil.createSimilarFile(localPath);
        task.onCreate();
        return task;
    }

    public static class Snapshot {
        /**
         * 下载任务 id，基于 httpUrl 计算得到，相同的资源具有相同的 id
         */
        public final String id;

        /**
         * 资源地址, http 格式
         */
        public final String httpUrl;

        /**
         * 资源本地保存路径
         */
        public final String localPath;

        /**
         * 资源长度, 可能 < 0
         */
        public final long contentLength;

        /**
         * 资源已经下载的长度, 总是 >= 0
         */
        public final long downloadLength;

        /**
         * 参考 http 协议，断点续传时用来辅助校验资源是否一致
         */
        public final long lastModify;

        /**
         * 该资源是否支持断点续传
         */
        public final boolean canContinue;

        /**
         * 下载状态
         */
        public final int status;

        private Snapshot(ADownloadTask task) {
            this.id = task.id;
            this.httpUrl = task.httpUrl;
            this.localPath = task.localPath;
            this.contentLength = task.contentLength;
            this.downloadLength = task.downloadLength;
            this.lastModify = task.lastModify;
            this.canContinue = task.canContinue;
            this.status = task.status;
        }

        public int getProgress() {
            try {
                Progress progress = new Progress(this.contentLength, this.downloadLength);
                return progress.getPercent();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return 0;
        }

    }

}
