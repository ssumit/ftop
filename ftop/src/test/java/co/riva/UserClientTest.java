package co.riva;

import olympus.common.JID;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

public class UserClientTest {

    @Test
    public void testAuth() throws ExecutionException, InterruptedException {
        UserClient userClient = new UserClient(new JID("go.to", "apollo", "64vkt615oooyyy1y"), "r4hh5u2hrdvay525y5dharaa2dv55r4a");
        userClient
                .connect()
                .thenCompose(UserClient::authenticate)
                .thenCompose(UserClient::disconnect)
                .toCompletableFuture().get();
    }
}