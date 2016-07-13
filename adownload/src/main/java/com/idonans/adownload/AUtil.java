package com.idonans.adownload;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.idonans.acommon.util.FileUtil;
import com.idonans.acommon.util.MD5Util;

import java.io.File;
import java.util.Date;

import okhttp3.Response;
import okhttp3.internal.http.HttpDate;

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

    /**
     * 获取该资源的原始长度，如果这是一个 part content, 则会取 Content-Range 中标识的原始资源长度，
     * 否则取 Content-Length 中的长度, 如果都没有，返回 -1
     */
    public static long getOriginalContentLength(Response response) {
        try {
            String contentRange = response.header("Content-Range", "").trim();
            if (!TextUtils.isEmpty(contentRange)) {
                // 100-22593/22594
                int index = contentRange.lastIndexOf('/');
                return Long.parseLong(contentRange.substring(index + 1));
            }

            String contentLength = response.header("Content-Length", "").trim();
            return Long.parseLong(contentLength);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1L;
    }

    /**
     * 判断指定资源是否支持断点续传
     */
    public static boolean canContinue(Response response) {
        try {
            String ranges = response.header("Accept-Ranges", "").trim();
            return "bytes".equalsIgnoreCase(ranges);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 获取资源的 last modify 值，如果失败，返回 -1
     */
    public static long getLastModify(Response response) {
        try {
            String lastModify = response.header("Last-Modified", "").trim();
            Date date = HttpDate.parse(lastModify);
            if (date != null) {
                return date.getTime();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1L;
    }

}
