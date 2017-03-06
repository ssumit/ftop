package co.riva;

import co.riva.group.GroupClient;
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
                .thenApply(UserClient::getGroupClient)
                .thenCompose(GroupClient::createGroup)
                .thenApply(GroupClient::getUserClient)
                .thenCompose(UserClient::disconnect)
                .toCompletableFuture().get();
    }

    @Test
    public void testGroupList() throws ExecutionException, InterruptedException {
        UserClient userClient = new UserClient(new JID("go.to", "apollo", "b9bz9xrlbfxl0rlb"), "altuuay333tlbw311y1wyhawb1lbuwba");
        userClient
                .connect()
                .thenCompose(UserClient::authenticate)
                .thenApply(UserClient::getGroupClient)
                .thenCompose(GroupClient::fetchAllGroups)
                .thenApply(GroupClient::getUserClient)
                .thenCompose(UserClient::disconnect)
                .toCompletableFuture().get();
    }
}