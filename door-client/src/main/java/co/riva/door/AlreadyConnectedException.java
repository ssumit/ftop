package co.riva.door;

public class AlreadyConnectedException extends RuntimeException {
    public AlreadyConnectedException(String message) {
        super(message);
    }
}