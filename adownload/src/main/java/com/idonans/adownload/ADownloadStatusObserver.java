package com.idonans.adownload;

import android.support.annotation.NonNull;

import com.idonans.acommon.lang.TaskQueue;
import com.idonans.acommon.lang.ThreadPool;
import com.idonans.acommon.lang.Threads;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by pengji on 16-7-13.
 */
public class ADownloadStatusObserver {

    private static final TaskQueue QUEUE = new TaskQueue(1);

    private static final Map<String, Long> LAST_NOTIFY_TIMES = new ConcurrentHashMap<>();

    private static long getLastNotifyTime(String id) {
        Long time = LAST_NOTIFY_TIMES.get(id);
        return time == null ? 0L : time;
    }

    private static void touchLastNotifyTime(String id) {
        LAST_NOTIFY_TIMES.put(id, System.currentTimeMillis());
    }

    /**
     * 此方法的调用频率会非常高，需要过滤。
     * <p/>
     * 例如：对同一个 id 的连续两次通知，那么第一次通知的可以忽略的。
     * <p/>
     * 但是，要注意，如果是对同一个 id 的连续多次通知(比如连续20次), 那么最佳方式可能是保留第一次和最后一次通知.
     * <p/>
     * 此处采取的策略是按照时间间隔计算，如果对同一个 id 的多次通知时间间隔太小，会忽略中间的通知，确保两次通知之间的间隔不低于一个阀值.
     * 同时要确保最后一个通知不会丢失
     */
    public static void notifyDownloadChanged(@NonNull String id) {
        QUEUE.enqueue(new ObserverRunnable(id));
    }

    private static final class ObserverRunnable implements Runnable {

        private static final long MIN_INTERVAL = 150L;

        @NonNull
        private final String mId;
        private final long mTime;

        private ObserverRunnable(@NonNull String id) {
            mId = id;
            mTime = System.currentTimeMillis();
        }

        @Override
        public void run() {
            final long lastNotifyTime = getLastNotifyTime(mId);
            final long interval = mTime - lastNotifyTime;
            if (interval >= MIN_INTERVAL) {
                touchLastNotifyTime(mId);
                // 立即通知
                notifyNow();
            } else if (interval < 0) {
                // 在延迟通知期间已经有其它通知了，忽略此次通知
                // ignore
            } else {
                // 延迟通知
                ThreadPool.getInstance().post(new Runnable() {
                    @Override
                    public void run() {
                        Threads.sleepQuietly(50L);
                        if (getLastNotifyTime(mId) == lastNotifyTime) {
                            notifyDownloadChanged(mId);
                        }
                    }
                });
            }
        }

        /**
         * 通知 observer for id
         */
        private void notifyNow() {
            // TODO
        }

    }

}
