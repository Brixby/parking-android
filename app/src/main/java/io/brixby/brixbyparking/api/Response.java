package io.brixby.parking.api;



public class Response {

    public static Response ERROR() {
        return error("error...");
    }

    private boolean isError;
    private String message;
    private Object data;

    private Response(boolean error, String message) {
        isError = error;
        this.message = message;
    }

    public static Response success(String message) {
        return new Response(false, message);
    }

    public static Response error(String message) {
        return new Response(true, message);
    }

    public String getMessage() {
        return message;
    }

    public boolean isError() {
        return isError;
    }

    public Object getData() {
        return data;
    }

    public Response setData(Object data) {
        this.data = data;
        return this;
    }

    public String toString() {
        return message + " - " + isError;
    }
}
