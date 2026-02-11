package dev.answer.iot;

/**
 * @author AnswerDev
 * @date 2025/11/23 01:55
 * @description IotParamsEvent
 */

public class IotParamsEvent {
    private boolean callBack;
    private long createTime;
    private String eventId;
    private Object extraData;
    private String resourceName;

    public IotParamsEvent() {
    }

    public IotParamsEvent(String str, Object obj, String str2, boolean z, long j) {
        this.resourceName = str;
        this.extraData = obj;
        this.eventId = str2;
        this.callBack = z;
        this.createTime = j;
    }

    public String getResourceName() {
        return this.resourceName;
    }

    public void setResourceName(String str) {
        this.resourceName = str;
    }

    public Object getExtraData() {
        return this.extraData;
    }

    public void setExtraData(Object obj) {
        this.extraData = obj;
    }

    public String getEventId() {
        return this.eventId;
    }

    public void setEventId(String str) {
        this.eventId = str;
    }

    public boolean isCallBack() {
        return this.callBack;
    }

    public void setCallBack(boolean z) {
        this.callBack = z;
    }

    public long getCreateTime() {
        return this.createTime;
    }

    public void setCreateTime(long j) {
        this.createTime = j;
    }
}