package com.github.jie65535.opencommand.json;

public final class JsonResponse {
    public int retcode = 200;
    public String message = "success";
    public Object data;

    public JsonResponse() {

    }

    public JsonResponse(int code, String message) {
        this.retcode = code;
        this.message = message;
    }

    public JsonResponse(int code, String message, Object data) {
        this.retcode = code;
        this.message = message;
        this.data = data;
    }

    public JsonResponse(Object data) {
        this.data = data;
    }
}
