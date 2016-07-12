package com.idonans.adownload;

/**
 * Created by pengji on 16-7-12.
 */
public class ADownloadTask {

    /**
     * 下载任务 id，基于 httpUrl 计算得到，相同的资源具有相同的 id
     */
    public String id;

    /**
     * 资源地址, http 格式
     */
    public String httpUrl;

    /**
     * 资源本地保存路径
     */
    public String localPath;

    /**
     * 资源长度, 可能 < 0
     */
    public long contentLength;

    /**
     * 资源已经下载的长度, 总是 >= 0
     */
    public long downloadLength;

    /**
     * 参考 http 协议，断点续传时用来辅助校验资源是否一致
     */
    public long lastModify;

    /**
     * 该资源是否支持断点续传
     */
    public boolean canContinue;

}
