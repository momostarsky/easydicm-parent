package com.easydicm.storescp.models;

public class RegInfo {

    private String clientId;
    private String clientName;
    private String pubkey;
    private int days;

    public RegInfo() {

    }

    public RegInfo(String clientId, String clientPubkey, String clientName, int days) {

           setClientId(clientId);
           setPubkey(clientPubkey);
           setDays(days);
           setClientName(clientName);
    }


    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getPubkey() {
        return pubkey;
    }

    public void setPubkey(String pubkey) {
        this.pubkey = pubkey;
    }

    public int getDays() {
        return days;
    }

    public void setDays(int days) {
        this.days = days;
    }
}
