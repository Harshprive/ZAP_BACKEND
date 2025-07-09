package com.ZAP_Backend.ZapServices.DataTransferObject;

public class LocationIQResponse {
//    @JsonProperty("display_name")
    private String displayName;
    private String lat;
    private String lon;

    public LocationIQResponse(String displayName, String lat, String lon) {
        this.displayName = displayName;
        this.lat = lat;
        this.lon = lon;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getLat() {
        return lat;
    }

    public void setLat(String lat) {
        this.lat = lat;
    }

    public String getLon() {
        return lon;
    }

    public void setLon(String lon) {
        this.lon = lon;
    }
}
