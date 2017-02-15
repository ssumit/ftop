package co.riva.group;

import co.riva.auth.SimpleDoorClient;
import co.riva.door.DoorEnvelopeType;
import co.riva.door.RequestMethod;
import com.google.gson.Gson;
import olympus.flock.messages.kronos.GroupConfiguration;
import olympus.flock.messages.kronos.GroupType;
import olympus.flock.messages.kronos.ProfileInfo;
import olympus.kronos.client.requests.CreateGroupRequest;
import olympus.message.types.Request;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

//for single request response
public class CreateGroupHelper {
    private CompletableFuture<String> responseFuture = new CompletableFuture<>();
    private CompletableFuture<String> notificationFuture = new CompletableFuture<>();
    private final SimpleDoorClient doorClient;
    private String requestID;

    public CreateGroupHelper(SimpleDoorClient doorClient) {
        this.doorClient = doorClient;
    }

    public CompletionStage<String> createGroup() {
        Request<CreateGroupRequest> groupRequest = createGroupRequest();
        groupRequest.to().setServiceName("groups");
        CreateGroupRequest payload = groupRequest.payload();
        requestID = payload.getId();
        SimpleDoorClient.MessageListener messageListener = getMessageListener();
        doorClient.addListener(messageListener);
        return doorClient.request(payload.getId(), payload, RequestMethod.CREATE_GROUP)
                .thenCompose(__ -> responseFuture);
    }

    private SimpleDoorClient.MessageListener getMessageListener() {
        return new SimpleDoorClient.MessageListener() {
            @Override
            public void onNewMessage(DoorEnvelopeType type, String message) {
                if (requestID != null && message.contains(requestID) && type.equals(DoorEnvelopeType.O_RESPONSE)) {
                    responseFuture.complete(message);
                } else if (type.equals(DoorEnvelopeType.O_MESSAGE) && message.contains("GROUP_UPDATE_NOTIFICATION")) {
                    notificationFuture.complete(message);
                }
                if (responseFuture.isDone() && notificationFuture.isDone()) {
                    doorClient.removeListener(this);
                }
            }

            @Override
            public void onErrorReceived(Throwable throwable) {
                responseFuture.completeExceptionally(throwable);
                notificationFuture.completeExceptionally(throwable);
                doorClient.removeListener(this);
            }
        };
    }

    private Request<CreateGroupRequest> createGroupRequest() {
        return new CreateGroupRequest.Builder()
                .id(UUID.randomUUID().toString())
                .profile(new ProfileInfo("w234e", null, "description"))
                .config(new GroupConfiguration(GroupType.close))
                .appDomain("go.to")
                .build();
    }

    public CompletableFuture<String> getNotificationFuture() {
        return notificationFuture;
    }
}