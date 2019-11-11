package com.ericsson.gerrit.plugins.eiffel.utils;

import io.restassured.response.ResponseBody;

public class GerritCommitResponse {
    final private String changeID;
    final private String legacyID;

    public GerritCommitResponse(ResponseBody responseBody) {
        changeID = responseBody.jsonPath().getMap("").get("change_id").toString();
        legacyID = responseBody.jsonPath().getMap("").get("_number").toString();
    }

    public String getChangeID() {
        return changeID;
    }

    public String getLegacyID() {
        return legacyID;
    }
}
