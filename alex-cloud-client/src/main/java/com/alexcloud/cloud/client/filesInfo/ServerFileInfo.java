package com.alexcloud.cloud.client.filesInfo;

public class ServerFileInfo {

    private String fileName;
    private long size;

    public String getFileName() {
        return fileName;
    }

    public long getSize() {
        return size;
    }

    public ServerFileInfo(String fileName, Long size) {
        this.fileName = fileName;
        this.size = size;
    }
}
