package util;

import java.util.LinkedList;

public class ConnectionsOfAllVersions extends LinkedList<ConnectionsOfTwoVersions>{


    private int postId;


    public ConnectionsOfAllVersions(int postId){
        this.postId = postId;
    }


    public int getPostId() {
        return postId;
    }

}
