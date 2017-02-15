package co.riva;

import olympus.common.JID;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

public class UserClientTest {

    @Test
    public void testAuth() throws ExecutionException, InterruptedException {
        UserClient userClient = new UserClient(new JID("go.to", "apollo", "b9bz9xrlbfxl0rlb"), "altuuay333tlbw311y1wyhawb1lbuwba");
        userClient
                .connect()
                .thenCompose(UserClient::authenticate)
                .thenCompose(__ -> {
                    return userClient.getGroupMessageHelper().createGroup()
                            .thenApply(___ -> userClient);
                })
                .thenCompose(UserClient::disconnect)
                .toCompletableFuture().get();
    }
}