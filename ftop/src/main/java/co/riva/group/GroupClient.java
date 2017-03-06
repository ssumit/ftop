package co.riva.group;

import co.riva.UserClient;
import co.riva.auth.SimpleDoorClient;

import java.util.concurrent.CompletionStage;

public class GroupClient {
    private final UserClient userClient;
    private final SimpleDoorClient doorClient;

    public GroupClient(UserClient userClient, SimpleDoorClient doorClient) {
        this.userClient = userClient;
        this.doorClient = doorClient;
    }

    //for convenience only
    public UserClient getUserClient() {
        return userClient;
    }

    public CompletionStage<GroupClient> createGroup() {
        CreateGroupHelper createGroupHelper = new CreateGroupHelper(doorClient);
        return createGroupHelper.createGroup()
                .thenCompose(____ -> createGroupHelper.getNotificationFuture())
                .thenApply(___ -> this);
    }

    public CompletionStage<GroupClient> updateRandomGroupName() {
        return null;
    }

    public CompletionStage<GroupClient> updateRandomGroupType() {
        return null;
    }

    public CompletionStage<GroupClient> addMemberToARandomGroup() {
        return null;
    }

    public CompletionStage<GroupClient> addMultipleMembersToARandomGroup() {
        return null;
    }

    public CompletionStage<GroupClient> addNonTeamMembersToRandomGroupByNonMemberAndFail() {
        return null;
    }

    public CompletionStage<GroupClient> addMemberToARandomGroupWithNoPrivilegesAndFail() {
        return null;
    }

    public CompletionStage<GroupClient> removeMemberFromARandomGroup() {
        return null;
    }

    public CompletionStage<GroupClient> removeMultipleMembersFromARandomGroup() {
        return null;
    }

    public CompletionStage<GroupClient> createAndFetchGroup() {
        return null;
    }

    public CompletionStage<GroupClient> fetchAllGroups() {
        FetchGroupListHelper fetchGroupListHelper = new FetchGroupListHelper(doorClient);
        return fetchGroupListHelper.fetchGroup()
                .thenApply(__ -> this);
    }
}