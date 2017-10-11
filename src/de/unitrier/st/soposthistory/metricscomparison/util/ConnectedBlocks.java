package de.unitrier.st.soposthistory.metricscomparison.util;

public class ConnectedBlocks {
    private Integer leftLocalId;
    private Integer rightLocalId;
    private int postBlockTypeId;

    public ConnectedBlocks(Integer leftLocalId, Integer rightLocalId, Integer postBlockTypeId) {
        this.leftLocalId = leftLocalId;
        this.rightLocalId = rightLocalId;
        this.postBlockTypeId = postBlockTypeId;
    }

    public Integer getLeftLocalId() {
        return leftLocalId;
    }

    public Integer getRightLocalId() {
        return rightLocalId;
    }

    public int getPostBlockTypeId() {
        return postBlockTypeId;
    }

    @Override
    public String toString() {
        return "(" + leftLocalId + ", " + rightLocalId + ")";
    }
}
