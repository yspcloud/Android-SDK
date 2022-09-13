package com.example.alan.sdkdemo.contact;

import java.io.Serializable;
import java.util.List;

public class ContactBean implements Serializable {
    private String departmentName;
    private String departmentId;
    private String usrOnlineStatu;
    private String usrId;
    private String usrNickName;
    private String usrLoginId;
    private String usrIsEndpoint;
    private String usrCuid;
    private boolean isPeople;
    private boolean isShowHead = false;
    private List<Integer> departmentIds;
    private boolean isChoose = false;
    private String email;
    private List<List<String>> departListAnother;

    private List<String> departFatherList;
    private String duties;
    private int onLineStatus;
    private String meetNum;
    private String sipKey;

    public String getSipKey() {
        return sipKey;
    }

    public void setSipKey(String sipKey) {
        this.sipKey = sipKey;
    }

    public String getMeetNum() {
        return meetNum;
    }

    public void setMeetNum(String meetNum) {
        this.meetNum = meetNum;
    }

    public int getOnLineStatus() {
        return onLineStatus;
    }

    public void setOnLineStatus(int onLineStatus) {
        this.onLineStatus = onLineStatus;
    }

    public String getDuties() {
        return duties;
    }

    public void setDuties(String duties) {
        this.duties = duties;
    }

    public List<String> getDepartFatherList() {
        return departFatherList;
    }

    public void setDepartFatherList(List<String> departFatherList) {
        this.departFatherList = departFatherList;
    }

    public List<List<String>> getDepartListAnother() {
        return departListAnother;
    }

    public void setDepartListAnother(List<List<String>> departListAnother) {
        this.departListAnother = departListAnother;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isChoose() {
        return isChoose;
    }

    public void setChoose(boolean chose) {
        isChoose = chose;
    }

    public List<Integer> getDepartmentIds() {
        return departmentIds;
    }

    public void setDepartmentIds(List<Integer> departmentIds) {
        this.departmentIds = departmentIds;
    }

    public boolean isShowHead() {
        return isShowHead;
    }

    public void setShowHead(boolean showHead) {
        isShowHead = showHead;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }

    public String getUsrOnlineStatu() {
        return usrOnlineStatu;
    }

    public void setUsrOnlineStatu(String usrOnlineStatu) {
        this.usrOnlineStatu = usrOnlineStatu;
    }

    public String getUsrId() {
        return usrId;
    }

    public void setUsrId(String usrId) {
        this.usrId = usrId;
    }

    public String getUsrNickName() {
        return usrNickName;
    }

    public void setUsrNickName(String usrNickName) {
        this.usrNickName = usrNickName;
    }

    public String getUsrLoginId() {
        return usrLoginId;
    }

    public void setUsrLoginId(String usrLoginId) {
        this.usrLoginId = usrLoginId;
    }

    public String getUsrIsEndpoint() {
        return usrIsEndpoint;
    }

    public void setUsrIsEndpoint(String usrIsEndpoint) {
        this.usrIsEndpoint = usrIsEndpoint;
    }

    public String getUsrCuid() {
        return usrCuid;
    }

    public void setUsrCuid(String usrCuid) {
        this.usrCuid = usrCuid;
    }

    public boolean isPeople() {
        return isPeople;
    }

    public void setPeople(boolean people) {
        isPeople = people;
    }

    @Override
    public String toString() {
        return "ContactBean{" +
                "departmentName='" + departmentName + '\'' +
                ", departmentId='" + departmentId + '\'' +
                ", usrOnlineStatu='" + usrOnlineStatu + '\'' +
                ", usrId='" + usrId + '\'' +
                ", usrNickName='" + usrNickName + '\'' +
                ", usrLoginId='" + usrLoginId + '\'' +
                ", usrIsEndpoint='" + usrIsEndpoint + '\'' +
                ", usrCuid='" + usrCuid + '\'' +
                ", isPeople=" + isPeople +
                ", isShowHead=" + isShowHead +
                ", departmentIds=" + departmentIds +
                ", isChoose=" + isChoose +
                ", email='" + email + '\'' +
                ", departListAnother=" + departListAnother +
                ", departFatherList=" + departFatherList +
                ", duties='" + duties + '\'' +
                ", onLineStatus=" + onLineStatus +
                ", meetNum='" + meetNum + '\'' +
                ", sipKey='" + sipKey + '\'' +
                '}';
    }
}
