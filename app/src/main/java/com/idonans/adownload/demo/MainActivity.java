package com.idonans.adownload.demo;

import android.os.Bundle;
import android.support.annotation.CheckResult;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.idonans.acommon.app.CommonActivity;
import com.idonans.acommon.lang.ThreadPool;
import com.idonans.acommon.lang.Threads;
import com.idonans.acommon.lang.WeakAvailable;
import com.idonans.acommon.util.FileUtil;
import com.idonans.acommon.util.ViewUtil;
import com.idonans.adownload.ADownload;
import com.idonans.adownload.ADownloadManager;
import com.idonans.adownload.ADownloadStatus;
import com.idonans.adownload.ADownloadTask;
import com.idonans.adownload.ADownloadTaskFetchCallback;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends CommonActivity {

    private static final String TAG = "MainActivity";

    private static final String TEST_URL = "http://www.iteye.com/images/logo.gif";
    private TextView mTaskInfo;
    private String mDownloadId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View addTask = ViewUtil.findViewByID(this, R.id.add_task);
        addTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addTask();
            }
        });

        View startTask = ViewUtil.findViewByID(this, R.id.start_task);
        startTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startTask();
            }
        });

        View pauseTask = ViewUtil.findViewByID(this, R.id.pause_task);
        pauseTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pauseTask();
            }
        });

        View stopTask = ViewUtil.findViewByID(this, R.id.stop_task);
        stopTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopTask();
            }
        });

        View removeTask = ViewUtil.findViewByID(this, R.id.remove_task);
        removeTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                removeTask();
            }
        });

        View removeAllTasks = ViewUtil.findViewByID(this, R.id.remove_all_tasks);
        removeAllTasks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                removeAllTasks();
            }
        });

        View printDebug = ViewUtil.findViewByID(this, R.id.print_debug);
        printDebug.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ADownloadManager.enqueuePrintDBContent();
            }
        });

        mTaskInfo = ViewUtil.findViewByID(this, R.id.task_info);
    }


    private void addTask() {
        File downloadDir = FileUtil.getPublicDownloadDir();
        if (downloadDir == null) {
            Toast.makeText(this, "public download dir not found", Toast.LENGTH_LONG).show();
            return;
        }

        mDownloadId = ADownload.download(TEST_URL, downloadDir.getAbsolutePath());
        bindDownloadTaskInfo(mDownloadId);
    }

    private void startTask() {
        ADownload.start(mDownloadId);
        bindDownloadTaskInfo(mDownloadId);
    }

    public void pauseTask() {
        ADownload.pause(mDownloadId);
        bindDownloadTaskInfo(mDownloadId);
    }

    public void stopTask() {
        ADownload.stop(mDownloadId);
        bindDownloadTaskInfo(mDownloadId);
    }

    public void removeTask() {
        ADownload.remove(mDownloadId);
        bindDownloadTaskInfo(mDownloadId);
    }

    public void removeAllTasks() {
        ADownload.removeAll();
    }

    private void bindDownloadTaskInfo(String downloadId) {
        new DownloadInfoBinder(mTaskInfo, downloadId).start();
    }

    private static class DownloadInfoBinder extends WeakAvailable implements Runnable, ADownloadTaskFetchCallback {

        private final String mDownloadId;

        public DownloadInfoBinder(TextView textView, String downloadId) {
            super(textView);
            mDownloadId = downloadId;
            textView.setTag(R.id.task_info, this);
        }

        private void start() {
            postToShow("...");
            startWithDelay(0L);
        }

        private void startWithDelay(long delay) {
            if (delay > 0) {
                Threads.sleepQuietly(delay);
            }
            ThreadPool.getInstance().post(this);
        }

        @Override
        public void run() {
            if (TextUtils.isEmpty(mDownloadId)) {
                // 没有下载任务 id
                postToShow("no download id");
                return;
            }

            ADownload.query(mDownloadId, this);
        }

        private boolean postToShow(final CharSequence info) {
            TextView textView = getAvailableTextView();
            if (textView == null) {
                return false;
            }

            Threads.runOnUi(new Runnable() {
                @Override
                public void run() {
                    TextView textView = getAvailableTextView();
                    if (textView != null) {
                        textView.setText(info);
                    }
                }
            });

            return true;
        }

        @CheckResult
        private TextView getAvailableTextView() {
            TextView textView = (TextView) getObject();
            if (isAvailable()) {
                return textView;
            }
            return null;
        }

        @Override
        public boolean isAvailable() {
            TextView textView = (TextView) getObject();
            if (textView == null) {
                return false;
            }
            if (textView.getTag(R.id.task_info) != this) {
                return false;
            }
            return super.isAvailable();
        }

        @Override
        public void onDownloadTaskFetched(ADownloadTask.Snapshot snapshot) {
            if (snapshot == null) {
                postToShow("download task not found for id " + mDownloadId);
                return;
            }

            StringBuilder builder = new StringBuilder();
            builder.append("Download id: " + snapshot.id);
            builder.append("\nLocal path: " + snapshot.localPath);
            builder.append("\nHttp url: " + snapshot.httpUrl);
            builder.append("\nProgress: [" + snapshot.downloadLength + "/" + snapshot.contentLength + "] " + snapshot.getProgress() + "%");
            builder.append("\nStatus: " + ADownloadStatus.getStatus(snapshot.status));
            builder.append("\nCan continue:" + snapshot.canContinue);
            builder.append("\nLast modify:" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(snapshot.lastModify)));

            if (postToShow(builder)) {
                // 正在下载或者等待下载的才需要跟循环踪显示状态
                if (snapshot.status == ADownloadStatus.STATUS_DOWNLOADING
                        || snapshot.status == ADownloadStatus.STATUS_IDLE) {
                    startWithDelay(800L);
                }
            }
        }
    }

}
