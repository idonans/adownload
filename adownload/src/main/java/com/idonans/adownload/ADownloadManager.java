package com.idonans.adownload;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.idonans.acommon.lang.CommonLog;
import com.idonans.acommon.lang.TaskQueue;
import com.idonans.acommon.lang.WeakAvailable;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Android Download Manager singleton
 * Created by pengji on 16-7-11.
 */
public class ADownloadManager {

    private static class InstanceHolder {

        private static final ADownloadManager sInstance = new ADownloadManager();

    }

    private static ADownloadManager getInstance() {
        return InstanceHolder.sInstance;
    }

    private static final String TAG = "ADownloadManager";
    private static final TaskQueue ACTION_QUEUE = new TaskQueue(1);
    @NonNull
    private final Info mInfo;

    private ADownloadManager() {
        String json = ADownloadDBManager.getInstance().getContent();
        mInfo = Info.fromJson(json);
        mInfo.onCreate();
    }

    /**
     * only for debug
     */
    public static void enqueuePrintDBContent() {
        enqueueStrongAction(new Action() {
            @Override
            public void onAction(@NonNull ADownloadManager manager, @NonNull Info info) {
                ADownloadDBManager.getInstance().printContent();
            }
        });
    }

    /**
     * 序列化下载管理器中的数据到数据库
     */
    public static void enqueueSave() {
        enqueueStrongAction(new Action() {
            @Override
            public void onAction(@NonNull ADownloadManager manager, @NonNull Info info) {
                CommonLog.d(TAG + " save to db");
                ADownloadDBManager.getInstance().setContent(info.toJson());
            }
        });
    }

    /**
     * @see #enqueueAction(Action, boolean)
     */
    public static void enqueueStrongAction(Action action) {
        enqueueAction(action, false);
    }

    /**
     * @see #enqueueAction(Action, boolean)
     */
    public static void enqueueAction(Action action) {
        enqueueAction(action, true);
    }

    /**
     * 对 ADownloadManager 的数据操作都是线性的，
     * 例如添加，暂停，删除下载任务。
     *
     * @param weak 指定内部是否对此 Action 只持弱引用
     */
    public static void enqueueAction(Action action, boolean weak) {
        ACTION_QUEUE.enqueue(new ActionRunnable(action, weak));
    }

    public interface Action {
        /**
         * 不要将 ADownloadManager, Info 对象及其内部对象引用到此方法之外
         */
        void onAction(@NonNull ADownloadManager manager, @NonNull Info info);
    }



    private static final Object sNotifyDownloadChangedLock = new Object();

    private static final class ActionRunnable extends WeakAvailable implements Runnable {

        private Action mStrongRef;

        public ActionRunnable(Action action, boolean weak) {
            super(action);
            if (!weak) {
                mStrongRef = action;
            }
        }

        @Override
        public final void run() {
            final ADownloadManager manager = ADownloadManager.getInstance();
            Action action = (Action) getObject();
            if (isAvailable()) {
                action.onAction(manager, manager.mInfo);
            }

            if (mStrongRef != null) {
                CommonLog.d(TAG + " ActionRunnable release strong ref");
                mStrongRef = null;
            }
        }

    }

    public static class Info {

        private static final String TAG = "ADownloadManager#Info";

        private List<ADownloadTask> mDownloadTasks;

        public void onCreate() {
            if (mDownloadTasks == null) {
                mDownloadTasks = new ArrayList<>();
            }

            for (ADownloadTask task : this.mDownloadTasks) {
                task.onCreate();
            }
        }

        @NonNull
        public List<ADownloadTask> getDownloadTasks() {
            return mDownloadTasks;
        }

        @NonNull
        public ADownloadTask findOrAddDownloadTask(@NonNull ADownloadRequest request) {
            ADownloadTask old = getDownloadTaskById(request.getId());
            if (old != null) {
                return old;
            }

            ADownloadTask task = ADownloadTask.create(request);
            mDownloadTasks.add(task);
            return task;
        }

        @CheckResult
        public ADownloadTask getDownloadTaskById(String id) {
            for (ADownloadTask task : this.mDownloadTasks) {
                if (task.id.equals(id)) {
                    return task;
                }
            }
            CommonLog.d(TAG + " download task not found for id " + id);
            return null;
        }

        @NonNull
        public static Info fromJson(@Nullable String json) {
            try {
                Gson gson = new Gson();
                Type type = new TypeToken<Info>() {
                }.getType();
                Info info = gson.fromJson(json, type);
                if (info != null) {
                    return info;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return new Info();
        }

        @Nullable
        public String toJson() {
            try {
                Gson gson = new Gson();
                Type type = new TypeToken<Info>() {
                }.getType();
                return gson.toJson(this, type);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

    }

}
