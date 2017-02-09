package co.riva.door;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

public class ThrowableUtils {

    private static final List<Class<? extends Exception>> WrapperExceptionClasses = Arrays.asList(ExecutionException.class, CompletionException.class);

    public static Throwable unwrap(Throwable throwable) {
        if (throwable != null) {
            while (throwable.getCause() != null && WrapperExceptionClasses.contains(throwable.getClass())) {
                throwable = throwable.getCause();
            }
        }
        return throwable;
    }
}