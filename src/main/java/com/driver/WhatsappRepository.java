package com.driver;

import java.util.*;

import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.RequestBody;

@Repository
public class WhatsappRepository {

    //Assume that each user belongs to at most one group
    //You can use the below mentioned hashmaps or delete these and create your own.
    private HashMap<Group, List<User>> groupUserMap;
    private HashMap<Group, List<Message>> groupMessageMap;
    private HashMap<Message, User> senderMap;
    private HashMap<Group, User> adminMap;
    private HashMap<String, User> userData;
    private int customGroupCount;
    private int messageId;

    public WhatsappRepository(){
        this.groupMessageMap = new HashMap<Group, List<Message>>();
        this.groupUserMap = new HashMap<Group, List<User>>();
        this.senderMap = new HashMap<Message, User>();
        this.adminMap = new HashMap<Group, User>();
        this.userData = new HashMap<>();
        this.customGroupCount = 0;
        this.messageId = 0;
    }

    public boolean isNewUser(String mobile) {
        if(userData.containsKey(mobile)) return false;
        return true;
    }

    public String createUser(String name, String mobile) throws Exception {
        if(!this.isNewUser(mobile)) {
            throw new Exception("User already exists");
        }
        userData.put(mobile, new User(name, mobile));
        return "SUCCESS";
    }

    public Group createGroup(List<User> users) {
        if(users.size() == 2) return this.createPersonalChat(users);

        this.customGroupCount++;
        String groupName = "Group " + this.customGroupCount;
        Group group = new Group(groupName, users.size());
        groupUserMap.put(group, users);
        adminMap.put(group, users.get(0));
        return group;
    }

    public Group createPersonalChat(List<User> users) {
        String groupName = users.get(1).getName();
        Group personalGroup = new Group(groupName, 2);
        groupUserMap.put(personalGroup, users);
        return personalGroup;
    }

    public int createMessage(String content){
        this.messageId++;
        Message message = new Message(messageId, content, new Date());
        return this.messageId;
    }

    public int sendMessage(Message message, User sender, Group group) throws Exception{
        if(!groupUserMap.containsKey(group)) throw new Exception("Group does not exist");
        if(!this.userExistsInGroup(group, sender)) throw  new Exception("You are not allowed to send message");

        List<Message> messages = new ArrayList<>();
        if(groupMessageMap.containsKey(group)) messages = groupMessageMap.get(group);

        messages.add(message);
        groupMessageMap.put(group, messages);
        senderMap.put(message, sender);
        return messages.size();
    }

    public boolean userExistsInGroup(Group group, User sender) {
        List<User> users = groupUserMap.get(group);
        for(User user: users) {
            if(user.equals(sender)) return true;
        }

        return false;
    }

    public String changeAdmin(User approver, User user, Group group) throws Exception{
        if(!groupUserMap.containsKey(group)) throw new Exception("Group does not exist");
        if(!adminMap.get(group).equals(approver)) throw new Exception("Approver does not have rights");
        if(!this.userExistsInGroup(group, user)) throw  new Exception("User is not a participant");

        adminMap.put(group, user);
        return "SUCCESS";
    }

    public int removeUser(User user) throws Exception {
        Group userRelatedGroup = null;
        for(Group group: groupUserMap.keySet()) {
            if(userExistsInGroup(group, user)) {
                userRelatedGroup = group;
                break;
            }
        }

        if(userRelatedGroup == null) throw new Exception("User not found");
        if(adminMap.get(userRelatedGroup).equals(user)) throw new Exception("Cannot remove admin");

        List<User> users = groupUserMap.get(userRelatedGroup);
        List<User> newUsers = new ArrayList<>();
        for (User u: users) {
            if(!u.equals(user)) newUsers.add(u);
        }

        groupUserMap.put(userRelatedGroup, users);
        HashSet<Message> userMessages = removeAndGetUserMessages(user);
        List<Message> updatedGroupMessages = new ArrayList<>();
        List<Message> oldGroupMessages = groupMessageMap.get(userRelatedGroup);
        for (Message message: oldGroupMessages) {
            if(!userMessages.contains(message))
                updatedGroupMessages.add(message);
        }

        groupMessageMap.put(userRelatedGroup, updatedGroupMessages);

        return groupUserMap.get(userRelatedGroup).size() + groupMessageMap.get(userRelatedGroup).size() + senderMap.size();
    }

    public HashSet<Message> removeAndGetUserMessages(User user) {
        HashSet<Message> messages = new HashSet<>();
        for(Message message: senderMap.keySet()) {
            if(senderMap.get(message).equals(user)) {
                messages.add(message);
                senderMap.remove(message);
            }
        }

        return messages;
    }

}
