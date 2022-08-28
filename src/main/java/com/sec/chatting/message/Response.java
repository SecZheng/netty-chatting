package com.sec.chatting.message;

import java.io.Serializable;

public class Response implements Serializable {
    private static final long serialVersionUID = -267086797005665273L;
    boolean success;
    String message;

    public Response(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "Response{" +
                "success=" + success +
                ", message='" + message + '\'' +
                '}';
    }
}
