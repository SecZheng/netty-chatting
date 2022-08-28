package com.sec.chatting.message;

import java.io.Serializable;
import java.util.Arrays;

public class Request implements Serializable {
    private static final long serialVersionUID = -8053770076455731094L;
    RequestType type;
    String[] parameters;

    public Request(RequestType type, String... parameters) {
        ensureParameters(type, parameters.length);
        this.type = type;
        this.parameters = parameters;
    }

    public RequestType getType() {
        return type;
    }

    public void setType(RequestType type) {
        this.type = type;
    }

    public String[] getParameters() {
        return parameters;
    }

    public void setParameters(String[] parameters) {
        this.parameters = parameters;
    }

    @Override
    public String toString() {
        return "Request{" +
                "type=" + type +
                ", parameters=" + Arrays.toString(parameters) +
                '}';
    }

    /**
     * login username password
     * send username message
     * gsend groupName message
     * create groupName u1 u2 ...
     * join groupName
     * quit groupName
     * exit
     */
    private void ensureParameters(RequestType type, int length) {
        switch (type) {
            case LOGIN:
            case SEND_ONE:
            case SEND_GROUP:
                if (length != 2) throw new RuntimeException(type.name() + "参数错误");
                break;
            case JOIN_GROUP:
            case QUIT_GROUP:
                if (length != 1) throw new RuntimeException(type.name() + "参数错误");
                break;
            case CREATE_GROUP:
            case EXIT:
                break;
            default:
                throw new RuntimeException("消息类型不存在");
        }
    }

    public static Request create(String command) {
        String[] strings = command.trim().split(" +");
        String[] parameters = Arrays.copyOfRange(strings, 1, strings.length);
        switch (strings[0]) {
            case "login":
                return new Request(RequestType.LOGIN, parameters);
            case "send":
                return new Request(RequestType.SEND_ONE, parameters);
            case "gsend":
                return new Request(RequestType.SEND_GROUP, parameters);
            case "join":
                return new Request(RequestType.JOIN_GROUP, parameters);
            case "quit":
                return new Request(RequestType.QUIT_GROUP, parameters);
            case "create":
                return new Request(RequestType.CREATE_GROUP, parameters);
            case "exit":
                return new Request(RequestType.EXIT, parameters);
            default:
                throw new RuntimeException("消息类型不存在");
        }
    }
}
