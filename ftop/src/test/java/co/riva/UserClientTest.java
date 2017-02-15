package co.riva;

import co.riva.group.GroupMessageHelper;
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
                    GroupMessageHelper groupMessageHelper = userClient.getGroupMessageHelper();
                    return groupMessageHelper.createGroup()
                            .thenCompose(____ -> groupMessageHelper.getNotificationFuture())
                            .thenApply(___ -> userClient);
                })
                .thenCompose(UserClient::disconnect)
                .toCompletableFuture().get();
    }
}