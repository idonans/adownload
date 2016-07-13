package com.idonans.adownload.demo;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.idonans.acommon.app.CommonActivity;
import com.idonans.acommon.lang.CommonLog;
import com.idonans.acommon.util.FileUtil;
import com.idonans.acommon.util.ViewUtil;
import com.idonans.adownload.ADownloadManager;
import com.idonans.adownload.ADownloadRequest;
import com.idonans.adownload.ADownloadTask;
import com.idonans.adownload.ADownloadTaskFetchCallback;

import java.io.File;

public class MainActivity extends CommonActivity {

    private static final String TAG = "MainActivity";
    private ADownloadRequest mDownloadRequest;

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

        View printDebug = ViewUtil.findViewByID(this, R.id.print_debug);
        printDebug.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ADownloadManager.enqueuePrintDBContent();
            }
        });
    }

    private void addTask() {
        final String url = "http://www.iteye.com/images/logo.gif?1448702469";
        File downloadDir = FileUtil.getPublicDownloadDir();
        if (downloadDir == null) {
            Toast.makeText(this, "public download dir not found", Toast.LENGTH_LONG).show();
            return;
        }

        mDownloadRequest = new ADownloadRequest(url, downloadDir.getAbsolutePath());
        mDownloadRequest.enqueueToDownload(new ADownloadTaskFetchCallback() {
            @Override
            public void onDownloadTaskFetched(ADownloadTask.Snapshot snapshot) {
                CommonLog.d(TAG + " onDownloadTaskFetched " + snapshot);
                mDownloadRequest = null;
            }
        });
    }

}
