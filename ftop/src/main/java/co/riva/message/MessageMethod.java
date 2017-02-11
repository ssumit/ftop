package co.riva.message;

public enum MessageMethod {
    INDIVIDUAL_CHAT("chatMessage"),

    CHAT_STATE("chatState"),

    RECEIPT("receipt"),

    DELETE_MESSAGE("deleteMessage"),

    ADD_ATTACHMENT("addAttachment"),

    GROUP_CHAT("groupMessage");

    private final String methodName;

    MessageMethod(String methodName) {
        this.methodName = methodName;
    }

    public String getMethodName() {
        return methodName;
    }
}
