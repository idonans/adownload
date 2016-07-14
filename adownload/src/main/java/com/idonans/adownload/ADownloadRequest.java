package com.idonans.adownload;

import android.support.annotation.NonNull;

import com.idonans.acommon.lang.Available;

/**
 * Created by pengji on 16-7-13.
 */
public class ADownloadRequest {

    private final String mId;
    private final String mHttpUrl;
    private final String mLocalPath;

    private DownloadAction mDownloadAction;

    public ADownloadRequest(String httpUrl, String baseDir) {
        mId = AUtil.generalIdByHttpUrl(httpUrl);
        mHttpUrl = httpUrl;
        mLocalPath = AUtil.generalLocalPath(baseDir, httpUrl);
    }

    @NonNull
    public String getId() {
        return mId;
    }

    String getHttpUrl() {
        return mHttpUrl;
    }

    String getLocalPath() {
        return mLocalPath;
    }

    /**
     * 添加到下载任务，并创建文件
     */
    public void enqueueToDownload(ADownloadTaskFetchCallback callback, boolean weak) {
        mDownloadAction = new DownloadAction(callback);
        ADownloadManager.enqueueAction(mDownloadAction, weak);
        ADownloadEngine.getInstance().notifyAppendDownloadTasks();
        ADownloadManager.enqueueSave();
    }

    private class DownloadAction implements Available, ADownloadManager.Action {

        private final ADownloadTaskFetchCallback mCallback;

        private DownloadAction(ADownloadTaskFetchCallback callback) {
            mCallback = callback;
        }

        @Override
        public void onAction(@NonNull ADownloadManager manager, @NonNull ADownloadManager.Info info) {
            ADownloadTask task = info.findOrAddDownloadTask(ADownloadRequest.this);
            if (mCallback != null) {
                mCallback.onDownloadTaskFetched(task.getSnapshot());
            }
        }

        @Override
        public boolean isAvailable() {
            return ADownloadRequest.this.mDownloadAction == this;
        }

    }

}
