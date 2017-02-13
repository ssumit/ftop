package co.riva.group;

import co.riva.UserClient;
import co.riva.door.DoorClient;
import co.riva.door.DoorEnvelopeType;
import com.google.gson.Gson;
import olympus.common.JID;
import olympus.flock.messages.kronos.GroupConfiguration;
import olympus.flock.messages.kronos.GroupType;
import olympus.flock.messages.kronos.ProfileInfo;
import olympus.kronos.client.requests.CreateGroupRequest;
import olympus.message.types.Request;
import olympus.message.types.ResponseListener;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class GroupMessageHelper implements UserClient.RequestListener {
    CompletableFuture<Void> fu = new CompletableFuture<>();
    private final DoorClient doorClient;
    private final Gson gson;

    public GroupMessageHelper(DoorClient doorClient) {
        this.doorClient = doorClient;
        gson = new Gson();
    }

    public CompletionStage<Void> createGroup() {
        Request<CreateGroupRequest> groupRequest = createGroupRequest();
        groupRequest.to().setServiceName("groups");
        String requestID = UUID.randomUUID().toString();
        System.out.println(requestID);
        doorClient.sendPacket(gson.toJson(groupRequest), DoorEnvelopeType.O_MESSAGE, "createGroup", requestID);
        return fu;
    }

    private Request<CreateGroupRequest> createGroupRequest() {
        return new CreateGroupRequest.Builder()
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