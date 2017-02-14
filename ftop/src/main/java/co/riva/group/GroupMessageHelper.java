package co.riva.group;

import co.riva.UserClient;
import co.riva.door.RequestMethod;
import co.riva.auth.SimpleDoorClient;
import olympus.flock.messages.kronos.GroupConfiguration;
import olympus.flock.messages.kronos.GroupType;
import olympus.flock.messages.kronos.ProfileInfo;
import olympus.kronos.client.requests.CreateGroupRequest;
import olympus.message.types.Request;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class GroupMessageHelper implements UserClient.RequestListener {
    CompletableFuture<Void> fu = new CompletableFuture<>();
    private final SimpleDoorClient doorClient;

    public GroupMessageHelper(SimpleDoorClient doorClient) {
        this.doorClient = doorClient;
    }

    public CompletionStage<Void> createGroup() {
        Request<CreateGroupRequest> groupRequest = createGroupRequest();
        groupRequest.to().setServiceName("groups");
        CreateGroupRequest payload = groupRequest.payload();
        doorClient.request(payload.getId(), payload, RequestMethod.CREATE_GROUP);
        return fu;
    }

    private Request<CreateGroupRequest> createGroupRequest() {
        return new CreateGroupRequest.Builder()
                .id(UUID.randomUUID().toString())
                .profile(new ProfileInfo("name", null, "description"))
                .config(new GroupConfiguration(GroupType.close))
                .appDomain("go.to")
                .build();
    }

    @Override
    public void onErrorReceived() {
        System.out.println();
        fu.completeExceptionally(new RuntimeException("on Error"));
    }

    @Override
    public void onNewMessage(String message) {
        System.out.println(message);
        fu.complete(null);
    }
}