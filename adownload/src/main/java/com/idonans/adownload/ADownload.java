package com.idonans.adownload;

import android.support.annotation.NonNull;

import com.idonans.acommon.lang.ThreadPool;
import com.idonans.acommon.lang.WeakAvailable;
import com.idonans.acommon.util.FileUtil;

/**
 * Created by pengji on 16-7-14.
 */
public class ADownload {

    /**
     * 下载指定资源到本地目录下, 返回下载任务的 id. 如果已经存在相同资源的下载任务，则不会做任何处理，否则新建一个下载任务并排队等待下载.
     */
    public static String download(String httpUrl, String localDir) {
        ADownloadRequest request = new ADownloadRequest(httpUrl, localDir);
        request.enqueueToDownload(new ADownloadTaskFetchCallback() {
            @Override
            public void onDownloadTaskFetched(ADownloadTask.Snapshot snapshot) {

            }
        }, false);
        return request.getId();
    }

    /**
     * 查询指定下载任务的信息， 注意，查询过程中对 callback 只是持有弱引用. 此 callback 支持 Available 校验.
     */
    public static void query(final String id, ADownloadTaskFetchCallback callback) {
        final WeakAvailable available = new WeakAvailable(callback);
        ADownloadManager.enqueueStrongAction(new ADownloadManager.Action() {
            @Override
            public void onAction(@NonNull ADownloadManager manager, @NonNull ADownloadManager.Info info) {
                ADownloadTask task = info.getDownloadTaskById(id);
                ADownloadTask.Snapshot snapshot = null;
                if (task != null) {
                    snapshot = task.getSnapshot();
                }
                ADownloadTaskFetchCallback c = (ADownloadTaskFetchCallback) available.getObject();
                if (available.isAvailable()) {
                    c.onDownloadTaskFetched(snapshot);
                }
            }
        });
    }

    /**
     * 开始下载一个任务(切换到等待下载)
     * <p/>
     * 如果当前任务已经完成，或者正在下载，或者正在排队等待下载，则不会做任何处理。
     * <p/>
     * 如果当前任务已经停止，则重新开始下载
     * <p/>
     * 如果当前任务处于出错状态，则重新开始下载
     * <p/>
     * 如果当前任务已经暂停，则使用断点的方式，续传下载（如果不能续传，会重新下载）
     */
    public static void start(final String id) {
        ADownloadManager.enqueueStrongAction(new ADownloadManager.Action() {
            @Override
            public void onAction(@NonNull ADownloadManager manager, @NonNull ADownloadManager.Info info) {
                ADownloadTask task = info.getDownloadTaskById(id);
                if (task == null) {
                    return;
                }

                switch (task.status) {
                    case ADownloadStatus.STATUS_COMPLETE:
                    case ADownloadStatus.STATUS_DOWNLOADING:
                    case ADownloadStatus.STATUS_IDLE:
                        // ignore
                        break;
                    case ADownloadStatus.STATUS_STOPPED:
                    case ADownloadStatus.STATUS_ERROR:
                        // 重新下载
                        task.canContinue = false;
                        task.status = ADownloadStatus.STATUS_IDLE;
                        task.createLocalFileIfNotExists();
                        break;
                    case ADownloadStatus.STATUS_PAUSED:
                    default:
                        // 续传下载
                        task.status = ADownloadStatus.STATUS_IDLE;
                        break;
                }
            }
        });
        ADownloadEngine.getInstance().notifyAppendDownloadTasks();
        ADownloadManager.enqueueSave();
    }

    /**
     * 暂停一个下载任务
     * <p/>
     * 如果当前任务正在下载，或者正在排队等待下载，则切换到暂停状态，否则不做任务处理
     */
    public static void pause(final String id) {
        ADownloadManager.enqueueStrongAction(new ADownloadManager.Action() {
            @Override
            public void onAction(@NonNull ADownloadManager manager, @NonNull ADownloadManager.Info info) {
                ADownloadTask task = info.getDownloadTaskById(id);
                if (task == null) {
                    return;
                }

                switch (task.status) {
                    case ADownloadStatus.STATUS_DOWNLOADING:
                    case ADownloadStatus.STATUS_IDLE:
                        // 暂停
                        task.status = ADownloadStatus.STATUS_PAUSED;
                        break;
                    case ADownloadStatus.STATUS_COMPLETE:
                    case ADownloadStatus.STATUS_STOPPED:
                    case ADownloadStatus.STATUS_ERROR:
                    case ADownloadStatus.STATUS_PAUSED:
                    default:
                        // ignore
                        break;
                }
            }
        });
        ADownloadManager.enqueueSave();
    }

    /**
     * 停止一个下载任务
     * <p/>
     * 如果当前任务已出错，或者已停止，则不做任何处理，否则切换停止状态。注意：已经完成的任务也可以切换到停止状态
     */
    public static void stop(final String id) {
        ADownloadManager.enqueueStrongAction(new ADownloadManager.Action() {
            @Override
            public void onAction(@NonNull ADownloadManager manager, @NonNull ADownloadManager.Info info) {
                ADownloadTask task = info.getDownloadTaskById(id);
                if (task == null) {
                    return;
                }

                switch (task.status) {
                    case ADownloadStatus.STATUS_ERROR:
                    case ADownloadStatus.STATUS_STOPPED:
                        // ignore
                        break;
                    case ADownloadStatus.STATUS_DOWNLOADING:
                    case ADownloadStatus.STATUS_IDLE:
                    case ADownloadStatus.STATUS_COMPLETE:
                    case ADownloadStatus.STATUS_PAUSED:
                    default:
                        // 停止
                        task.status = ADownloadStatus.STATUS_STOPPED;
                        break;
                }
            }
        });
        ADownloadManager.enqueueSave();
    }

    /**
     * 删除一个下载任务
     * <p/>
     * 删除指定下载任务，同时会删除相关的文件。在删除任务时，会先将任务停止，然后删除。
     */
    public static void remove(final String id) {
        ADownloadManager.enqueueStrongAction(new ADownloadManager.Action() {
            @Override
            public void onAction(@NonNull ADownloadManager manager, @NonNull ADownloadManager.Info info) {
                ADownloadTask task = info.getDownloadTaskById(id);
                if (task == null) {
                    return;
                }

                switch (task.status) {
                    case ADownloadStatus.STATUS_ERROR:
                    case ADownloadStatus.STATUS_STOPPED:
                        // ignore
                        break;
                    case ADownloadStatus.STATUS_DOWNLOADING:
                    case ADownloadStatus.STATUS_IDLE:
                    case ADownloadStatus.STATUS_COMPLETE:
                    case ADownloadStatus.STATUS_PAUSED:
                    default:
                        // 停止
                        task.status = ADownloadStatus.STATUS_STOPPED;
                        break;
                }

                info.getDownloadTasks().remove(task);
                pendingToRemove(task.getSnapshot());
            }
        });
        ADownloadManager.enqueueSave();
    }

    private static final void pendingToRemove(final ADownloadTask.Snapshot snapshot) {
        ThreadPool.getInstance().post(new Runnable() {
            @Override
            public void run() {
                FileUtil.deleteFileQuietly(snapshot.localPath);
            }
        });
    }

}
