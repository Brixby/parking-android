package io.brixby.parking.api.response;


public class MppResponse {

    private boolean isOk;
    private String errorInfo;
    private String errorCode;
    private boolean isNetworkError;

    void setOk(boolean isOk) {
        this.isOk = isOk;
    }

    void setNetworkError(String errorMessage) {
        isOk = false;
        isNetworkError = true;
        errorInfo = errorMessage;
    }

    public boolean isOk() {
        return isOk;
    }

    public String getErrorInfo() {
        return errorInfo;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public boolean isNetworkError() {
        return isNetworkError;
    }
}
