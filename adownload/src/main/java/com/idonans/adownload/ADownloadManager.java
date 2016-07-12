package com.idonans.adownload;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.idonans.acommon.lang.CommonLog;
import com.idonans.acommon.lang.TaskQueue;
import com.idonans.acommon.lang.WeakAvailable;

import java.lang.reflect.Type;

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

    private static final String TAG = "ADownloadManager";
    private final TaskQueue mActionQueue = new TaskQueue(1);
    @NonNull
    private final Info mInfo;

    private ADownloadManager() {
        String json = ADownloadDBManager.getInstance().get();
        mInfo = Info.fromJson(json);
    }

    /**
     * enqueueAction with weak true
     *
     * @see #enqueueAction(Action, boolean)
     */
    public void enqueueAction(Action action) {
        enqueueAction(action, true);
    }

    /**
     * 对 ADownloadManager 的数据操作都是线性的，
     * 例如添加，暂停，删除下载任务。
     *
     * @param weak 指定内部是否对此 Action 只持弱引用
     */
    public void enqueueAction(Action action, boolean weak) {
        mActionQueue.enqueue(new ActionRunnable(action, weak));
    }

    /**
     * only for debug
     */
    public void printDebugDatabaseContentAsync() {
        enqueueAction(new Action() {
            @Override
            public void onAction(@NonNull ADownloadManager manager, @NonNull Info info) {
                ADownloadDBManager.getInstance().printContent();
            }
        }, false);
    }

    /**
     * 序列化下载管理器中的数据到数据库
     */
    public void saveAsync() {
        enqueueAction(new Action() {
            @Override
            public void onAction(@NonNull ADownloadManager manager, @NonNull Info info) {
                CommonLog.d(TAG + " save to db");
                ADownloadDBManager.getInstance().set(info.toJson());
            }
        }, false);
    }

    public interface Action {
        /**
         * 不要将 ADownloadManager, Info 对象及其内部对象引用到此方法之外
         */
        void onAction(@NonNull ADownloadManager manager, @NonNull Info info);
    }

    private final class ActionRunnable extends WeakAvailable implements Runnable {

        private Action mStrongRef;

        public ActionRunnable(Action action, boolean weak) {
            super(action);
            if (!weak) {
                mStrongRef = action;
            }
        }

        @Override
        public final void run() {
            Action action = (Action) getObject();
            if (isAvailable()) {
                action.onAction(ADownloadManager.this, ADownloadManager.this.mInfo);
            }

            if (mStrongRef != null) {
                CommonLog.d(TAG + " ActionRunnable release strong ref");
                mStrongRef = null;
            }
        }

    }

    public static class Info {

        private static final String TAG = "ADownloadManager#Info";
        private static final int VERSION = 1;

        public int version = VERSION;

        /**
         * 校验数据版本，可能当前恢复的数据是以前格式的数据，需要转换
         */
        private boolean validateVersion() {
            CommonLog.d(TAG + " validateVersion [" + this.version + ", " + VERSION + "]");
            if (this.version == VERSION) {
                CommonLog.d(TAG + " version match");
                return true;
            } else if (this.version < VERSION) {
                CommonLog.d(TAG + " try up version " + this.version + "->" + VERSION);
                if (this.version < 1) {
                    CommonLog.d(TAG + " invalid version " + this.version);
                    return false;
                }

                // TODO
                // 数据版本升级
                return true;
            } else {
                CommonLog.d(TAG + " invalid version " + this.version);
                return false;
            }
        }

        @NonNull
        public static Info fromJson(@Nullable String json) {
            try {
                Gson gson = new Gson();
                Type type = new TypeToken<Info>() {
                }.getType();
                Info info = gson.fromJson(json, type);
                if (info != null && info.validateVersion()) {
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
