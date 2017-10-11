package util;

import java.util.LinkedList;

public class ConnectionsOfTwoVersions extends LinkedList<ConnectedBlocks> {

    int leftVersionId;
    int rightVersionId;

    public ConnectionsOfTwoVersions(int leftVersionId){
        this.leftVersionId = leftVersionId;
        this.rightVersionId = leftVersionId + 1;
    }
}