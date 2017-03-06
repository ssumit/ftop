package co.riva;

import olympus.common.JID;
import olympus.kronos.client.util.FutureUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

public class MultiUserSimulator {

    private List<UserClient> clients = new ArrayList<>();

    public MultiUserSimulator(Map<JID, String> userJIDToAuthToken) {
        clients = userJIDToAuthToken.keySet().stream()
                .map(userJID -> new UserClient(userJID, userJIDToAuthToken.get(userJID))).collect(Collectors.toList());
    }

    public CompletionStage<MultiUserSimulator> connect() {
        return null;
    }

    public CompletionStage<MultiUserSimulator> testOneToOneMessagesForAll(int numberOfUsers) {
        return null;
    }

    public CompletionStage<MultiUserSimulator> testGroupMessagesForAll(int numberOfUsers) {
        return null;
    }

    public CompletionStage<MultiUserSimulator> testGroupMemberAdditionNotificationForAll(int numberOfUsers) {
        return null;
    }

    public CompletionStage<MultiUserSimulator> testGroupNameUpdateNotificationForAll(int numberOfUsers) {
        return null;
    }

    public CompletionStage<MultiUserSimulator> disConnect(int numberOfUsers) {
        return null;
    }
}