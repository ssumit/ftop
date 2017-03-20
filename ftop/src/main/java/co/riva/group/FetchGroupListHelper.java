package co.riva.group;

import co.riva.auth.SimpleDoorClient;
import co.riva.door.DoorEnvelopeType;
import co.riva.door.RequestMethod;
import olympus.kronos.client.helpers.constants.GroupTrait;
import olympus.kronos.client.requests.FetchGroupListRequest;
import olympus.message.types.Request;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class FetchGroupListHelper {
    private final SimpleDoorClient doorClient;
    private CompletableFuture<String> responseFuture = new CompletableFuture<>();
    private String requestID;

    public FetchGroupListHelper(SimpleDoorClient doorClient) {
        this.doorClient = doorClient;
    }

    public CompletionStage<String> fetchGroup() {
        Request<FetchGroupListRequest> groupRequest = fetchGroupRequest();
        groupRequest.to().setServiceName("groups");
        FetchGroupListRequest payload = groupRequest.payload();
        requestID = payload.getId();
        SimpleDoorClient.MessageListener messageListener = getMessageListener();
        doorClient.addListener(messageListener);
        return doorClient.request(payload.getId(), payload, RequestMethod.FETCH_GROUP_LIST)
                .thenCompose(__ -> responseFuture);
    }

    private Request<FetchGroupListRequest> fetchGroupRequest() {
        Request<FetchGroupListRequest> groupRequest = new FetchGroupListRequest.Builder()
                .id(UUID.randomUUID().toString())
                .appDomain("go.to")
                .filter(Arrays.asList(GroupTrait.joined, GroupTrait.open))
                .build();
        groupRequest.to().setServiceName("groups");
        return groupRequest;
    }

    private SimpleDoorClient.MessageListener getMessageListener() {
        return new SimpleDoorClient.MessageListener() {
            @Override
            public void onNewMessage(DoorEnvelopeType type, String message) {
                if (requestID != null && message.contains(requestID) && type.equals(DoorEnvelopeType.O_RESPONSE)) {
                    responseFuture.complete(message);
                    doorClient.removeListener(this);
                }
            }

            @Override
            public void onErrorReceived(Throwable throwable) {
                responseFuture.completeExceptionally(throwable);
                doorClient.removeListener(this);
            }
        };
    }
}