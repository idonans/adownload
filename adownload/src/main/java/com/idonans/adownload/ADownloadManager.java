package com.idonans.adownload;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

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

    @NonNull
    private final Info mInfo;

    private ADownloadManager() {
        String json = DownloadDBManager.getInstance().get();
        mInfo = Info.fromJson(json);
    }

    @NonNull
    public Info getInfo() {
        return mInfo;
    }

    public void saveInfo() {
        // 使用两个队列 ？ (主从队列处理序列化冲突？相比于 lock 或者 copyOnWrite 是否与偶更高的效率?)
    }

    public static class Info {

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
