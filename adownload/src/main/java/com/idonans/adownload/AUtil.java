package com.idonans.adownload;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.idonans.acommon.util.FileUtil;
import com.idonans.acommon.util.MD5Util;

import java.io.File;

/**
 * Created by pengji on 16-7-13.
 */
public final class AUtil {


    /**
     * 根据 httpUrl 生成 id
     */
    @NonNull
    public static String generalIdByHttpUrl(String httpUrl) {
        return MD5Util.md5(httpUrl);
    }

    /**
     * 根据 httpUrl 生成本地文件路径
     */
    @CheckResult
    public static String generalLocalPath(String baseDir, String httpUrl) {
        if (TextUtils.isEmpty(baseDir)) {
            return null;
        }

        String filename = FileUtil.getFilenameFromUrl(httpUrl);
        if (TextUtils.isEmpty(filename)) {
            return null;
        }

        return new File(baseDir, filename).getAbsolutePath();
    }


    @CheckResult
    public static String createSimilarFile(String path) {
        try {
            File f = new File(path);
            File parent = f.getParentFile();
            String filename = f.getName();

            String extension = FileUtil.getFileExtensionFromUrl(filename);
            if (!TextUtils.isEmpty(extension)) {
                filename = filename.substring(0, filename.length() - extension.length() - 1);
                extension = "." + extension;
            } else {
                extension = "";
            }

            if (FileUtil.createDir(parent)) {
                File tmpFile = new File(parent, filename + extension);

                if (tmpFile.createNewFile()) {
                    return tmpFile.getAbsolutePath();
                }

                for (int i = 1; i < 20; i++) {
                    tmpFile = new File(parent, filename + "(" + i + ")" + extension);
                    if (tmpFile.createNewFile()) {
                        return tmpFile.getAbsolutePath();
                    }
                }

                throw new RuntimeException("相似文件太多 " + tmpFile.getAbsolutePath());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
