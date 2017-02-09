package co.riva.door;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class FutureUtils {
    public static <N> CompletableFuture<N> getFailedFuture(Throwable e) {
        CompletableFuture<N> future = new CompletableFuture<>();
        future.completeExceptionally(e);
        return future;
    }

    public static BiConsumer<Object, Throwable> thenOnException(Consumer<Throwable> throwableConsumer) {
        return (o, throwable) -> {
            if (throwable != null) {
                throwableConsumer.accept(ThrowableUtils.unwrap(throwable));
            }
        };
    }

    public static <T> CompletionStage<List<T>> allOf(final List<CompletionStage<T>> futures) {
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]))
                .thenApply(__ -> {
                    final List<T> list = new ArrayList<>();
                    futures.forEach(future -> future.thenAccept(list::add));
                    return list;
                });
    }
}