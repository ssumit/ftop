package co.riva.door;

public enum RequestMethod {
    CREATE_GROUP("123@groups.go.to", "createGroup");

    private final String to;
    private final String methodName;

    RequestMethod(String to, String methodName) {
        this.to = to;
        this.methodName = methodName;
    }

    public String to() {
        return to;
    }

    public String methodName() {
        return methodName;
    }
}
