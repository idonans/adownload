package com.idonans.adownload;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.idonans.acommon.data.OkHttpManager;
import com.idonans.acommon.lang.CommonLog;
import com.idonans.acommon.lang.Progress;
import com.idonans.acommon.lang.TaskQueue;
import com.idonans.acommon.lang.WeakAvailable;
import com.idonans.acommon.util.IOUtil;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Date;
import java.util.List;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.http.HttpDate;

/**
 * 下载引擎
 * Created by pengji on 16-7-13.
 */
public class ADownloadEngine {

    private static class InstanceHolder {

        private static final ADownloadEngine sInstance = new ADownloadEngine();

    }

    public static ADownloadEngine getInstance() {
        return InstanceHolder.sInstance;
    }

    private final TaskQueue mDownloadQueue = new TaskQueue(1);

    private ADownloadEngine() {
    }

    public void setMaxThreads(int maxThreads) {
        mDownloadQueue.setMaxCount(maxThreads);
    }

    public int getMaxThreads() {
        return mDownloadQueue.getMaxCount();
    }

    public int getRunningThreads() {
        return mDownloadQueue.getCurrentCount();
    }

    /**
     * 将所有排队中的任务添加到下载队列里
     */
    public void notifyAppendDownloadTasks() {
        ADownloadManager.enqueueStrongAction(new ADownloadManager.Action() {
            @Override
            public void onAction(@NonNull ADownloadManager manager, @NonNull ADownloadManager.Info info) {
                List<ADownloadTask> tasks = info.getDownloadTasks();
                for (ADownloadTask task : tasks) {
                    if (task.status == ADownloadStatus.STATUS_IDLE) {
                        task.status = ADownloadStatus.STATUS_DOWNLOADING;
                        mDownloadQueue.enqueue(new DownloadRunner(task));
                    }
                }
            }
        });
    }

    public final class DownloadRunner extends WeakAvailable implements Runnable {

        private static final String TAG = "DownloadRunner";
        private final String mId;
        private boolean mCalled;

        public DownloadRunner(ADownloadTask task) {
            super(task);
            mId = task.id;
            task.setDownloadRunner(this);
        }

        private ADownloadTask getDownloadTask() {
            return (ADownloadTask) getObject();
        }

        private ADownloadTask.Snapshot getDownloadTaskSnapshot() {
            ADownloadTask task = getDownloadTask();
            if (task != null) {
                return task.getSnapshot();
            }
            return null;
        }

        @Override
        public void run() {
            if (mCalled) {
                CommonLog.d(TAG + " already call run");
                new RuntimeException().printStackTrace();
                return;
            }
            mCalled = true;

            RandomAccessFile randomAccessFile = null;
            Response response = null;
            try {
                ADownloadTask.Snapshot snapshot = getDownloadTaskSnapshot();
                checkDownloadingOrThrow();

                CommonLog.d(TAG + " start download " + snapshot.httpUrl + " -> " + snapshot.localPath + " " + mId);

                // create download file
                String localPath = snapshot.localPath;
                if (TextUtils.isEmpty(localPath)) {
                    CommonLog.d(TAG + " download task local path is empty. " + mId);
                    setDownloadError();
                    return;
                }

                File targetFile = new File(localPath);
                if (!targetFile.exists()) {
                    CommonLog.d(TAG + " download task local path not exists " + mId);
                    setDownloadError();
                    return;
                }

                if (!targetFile.isFile()) {
                    CommonLog.d(TAG + " download task local path is not a file " + mId);
                    setDownloadError();
                    return;
                }

                randomAccessFile = new RandomAccessFile(targetFile, "rwd");
                snapshot = getDownloadTaskSnapshot();
                checkDownloadingOrThrow();

                // 尝试断点续传
                if (randomAccessFile.length() != snapshot.downloadLength /*文件发生了变更, 或者在系统杀死当前进程前未序列化数据, 保守逻辑, 不要续传*/
                        || !snapshot.canContinue /*该资源不支持续传*/
                        || snapshot.contentLength <= 0 /*资源长度未知, 保守逻辑, 不要续传*/
                        || snapshot.lastModify <= 0 /*资源 last modify 未知, 保守逻辑, 不要续传*/) {
                    // 重新开始下载
                    randomAccessFile.setLength(0L);
                    randomAccessFile.seek(0L);
                    resetDownloadContent();
                }

                snapshot = getDownloadTaskSnapshot();
                checkDownloadingOrThrow();

                OkHttpClient client = OkHttpManager.getInstance().getOkHttpClient();
                Request.Builder builder = new Request.Builder();
                if (snapshot.canContinue) {
                    CommonLog.d(TAG + " continue download [" + snapshot.downloadLength + "/" + snapshot.contentLength + "] " + mId);
                    builder.addHeader("Range", "bytes=" + snapshot.downloadLength);
                    builder.addHeader("If-Range", HttpDate.format(new Date(snapshot.lastModify)));
                }
                builder.url(snapshot.httpUrl);
                builder.get();
                Request request = builder.build();

                Call call = client.newCall(request);
                response = call.execute();
                int code = response.code();

                snapshot = getDownloadTaskSnapshot();
                checkDownloadingOrThrow();

                if (code == 200) {
                    // 这是一个从头开始的下载
                    randomAccessFile.setLength(0L);
                    randomAccessFile.seek(0L);

                    {
                        ADownloadTask task = getDownloadTask();
                        checkDownloadingOrThrow();

                        task.canContinue = AUtil.canContinue(response);
                        task.downloadLength = 0L;
                        task.contentLength = AUtil.getOriginalContentLength(response);
                        task.lastModify = AUtil.getLastModify(response);

                        task = null;
                    }

                } else if (code == 206) {
                    // 这是一个断点续传
                    if (randomAccessFile.length() != snapshot.downloadLength) {
                        throw new IllegalAccessException("fail continue write content, length not match file length:" + randomAccessFile.length() + ", download length:" + snapshot.downloadLength + " " + mId);
                    }
                    if (snapshot.contentLength != AUtil.getOriginalContentLength(response)) {
                        throw new IllegalAccessException("fail continue write content, content length not math old:" + snapshot.contentLength + ", this:" + AUtil.getOriginalContentLength(response) + " " + mId);
                    }
                    if (snapshot.lastModify != AUtil.getLastModify(response)) {
                        throw new IllegalAccessException("fail continue write content, last modify not math old:" + snapshot.lastModify + ", this:" + AUtil.getLastModify(response) + " " + mId);
                    }

                    randomAccessFile.seek(snapshot.downloadLength);
                } else {
                    setDownloadError();
                }

                snapshot = getDownloadTaskSnapshot();
                checkDownloadingOrThrow();

                // write content
                Progress progress = new Progress(snapshot.contentLength, snapshot.downloadLength) {
                    @Override
                    protected void onUpdate() {
                        ADownloadTask task = getDownloadTask();
                        if (isDownloading()) {
                            // 此方法会校验进度返回的合法性
                            int percent = getPercent();
                            CommonLog.d(TAG + " download progress " + percent + "% " + mId);
                            task.downloadLength = this.getCurrent();
                            notifyDownloadChanged();
                        }
                    }
                };

                final RandomAccessFile raf = randomAccessFile;
                long copy = IOUtil.copy(response.body().byteStream(), new OutputStream() {
                    @Override
                    public void write(byte[] b, int off, int len) throws IOException {
                        raf.write(b, off, len);
                    }

                    @Override
                    public void write(int i) throws IOException {
                        throw new UnsupportedOperationException();
                    }
                }, DownloadRunner.this, progress);
                CommonLog.d(TAG + " copy size " + copy + " " + mId);

                snapshot = getDownloadTaskSnapshot();
                checkDownloadingOrThrow();
                if (snapshot.contentLength <= 0 ||
                        (snapshot.contentLength == snapshot.downloadLength
                                && randomAccessFile.length() == snapshot.downloadLength)) {
                    setDownloadComplete();
                } else {
                    CommonLog.d(TAG + " length not match snapshot contentLength:" + snapshot.contentLength
                            + ", snapshot downloadLength:" + snapshot.downloadLength
                            + ", local file length:" + randomAccessFile.length());
                    setDownloadError();
                }
            } catch (Exception e) {
                e.printStackTrace();
                setDownloadError();
            } finally {
                IOUtil.closeQuietly(response);
                IOUtil.closeQuietly(randomAccessFile);
                notifyDownloadChanged();

                // 当前一个下载任务结束时同步一次数据到磁盘
                ADownloadManager.enqueueSave();
            }
        }

        private void resetDownloadContent() {
            ADownloadTask task = getDownloadTask();
            if (isDownloading()) {
                task.canContinue = false;
                task.downloadLength = 0L;
                task.contentLength = 0L;
                task.lastModify = 0L;
            }
        }

        private void setDownloadError() {
            ADownloadTask task = getDownloadTask();
            if (isDownloading()) {
                task.status = ADownloadStatus.STATUS_ERROR;
            }
        }

        private void setDownloadComplete() {
            ADownloadTask task = getDownloadTask();
            if (isDownloading()) {
                task.status = ADownloadStatus.STATUS_COMPLETE;
            }
        }

        private void checkDownloadingOrThrow() throws Exception {
            if (!isDownloading()) {
                String message = TAG + " cancel to run download task, status is not downloading or download runner has changed. #" + mId;
                throw new IllegalStateException(message);
            }
        }

        public boolean isDownloading() {
            return isDownloadStatus(ADownloadStatus.STATUS_DOWNLOADING);
        }

        public boolean isDownloadStatus(int targetStatus) {
            ADownloadTask task = getDownloadTask();
            if (isAvailable()) {
                return task.status == targetStatus;
            }
            return false;
        }

        @Override
        public boolean isAvailable() {
            ADownloadTask task = getDownloadTask();
            return task != null
                    && super.isAvailable()
                    && task.getDownloadRunner() == this;
        }

        private void notifyDownloadChanged() {
            // 此方法的调用频率会很高，当前下载任务的状态发生了变更 mId
        }

    }

}
