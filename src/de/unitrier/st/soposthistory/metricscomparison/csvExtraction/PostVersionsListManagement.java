package de.unitrier.st.soposthistory.metricscomparison.csvExtraction;

import de.unitrier.st.soposthistory.blocks.CodeBlockVersion;
import de.unitrier.st.soposthistory.blocks.TextBlockVersion;
import de.unitrier.st.soposthistory.metricscomparison.util.ConnectedBlocks;
import de.unitrier.st.soposthistory.metricscomparison.util.ConnectionsOfAllVersions;
import de.unitrier.st.soposthistory.metricscomparison.util.ConnectionsOfTwoVersions;
import de.unitrier.st.soposthistory.urls.Link;
import de.unitrier.st.soposthistory.version.PostVersion;
import de.unitrier.st.soposthistory.version.PostVersionList;

import java.io.File;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public class PostVersionsListManagement {

    private String pathToDirectory;

    public static Pattern pattern_groundTruth = Pattern.compile("[0-9]+" + "\\.csv");
    public List<PostVersionList> postVersionLists = new LinkedList<>();


    // constructor
    public PostVersionsListManagement(String pathToDirectoryOfPostHistories) {
        parseAllPostVersionLists(pathToDirectoryOfPostHistories);
    }


    private void parseAllPostVersionLists(String pathToDirectoryOfPostHistories) {
        this.pathToDirectory = pathToDirectoryOfPostHistories;

        File file = new File(pathToDirectoryOfPostHistories);
        File[] allPostHistoriesInFolder = file.listFiles((dir, name) -> name.matches(pattern_groundTruth.pattern())); // https://stackoverflow.com/questions/4852531/find-files-in-a-folder-using-java

        assert allPostHistoriesInFolder != null;
        for (File postHistory : allPostHistoriesInFolder) {
            PostVersionList tmpPostVersionList = new PostVersionList();
            int postId = Integer.valueOf(postHistory.getName().substring(0, postHistory.getName().length() - 4));

            tmpPostVersionList.readFromCSV(pathToDirectory, postId, 2, false);

            Link.normalizeLinks(tmpPostVersionList);
            // removeEmptyTextAndCodeBlocks(tmpPostVersionList);

            postVersionLists.add(
                    tmpPostVersionList
            );
        }

        postVersionLists.sort(Comparator.comparingInt(o -> o.getFirst().getPostId()));
    }

    // access to post verion lists
    public PostVersionList getPostVersionListWithID(int postID) {
        for (PostVersionList postVersionList : postVersionLists) {
            if (postID == postVersionList.getFirst().getPostId()) {
                return postVersionList;
            }
        }

        return null;
    }


    // converting post verion lists to make them easier to compare with ground truth
    private ConnectionsOfTwoVersions getAllConnectionsBetweenTwoVersions_text(int leftVersionId, PostVersion leftPostVersion, PostVersion rightPostVersion) {
        ConnectionsOfTwoVersions connectionsOfTwoVersions = new ConnectionsOfTwoVersions(leftVersionId);

        for (int i = 0; i < rightPostVersion.getPostBlocks().size(); i++) {
            if (rightPostVersion.getPostBlocks().get(i) instanceof CodeBlockVersion)
                continue;

            int rightLocalId = rightPostVersion.getPostBlocks().get(i).getLocalId();
            Integer leftLocalId = null;
            if (rightPostVersion.getPostBlocks().get(i).getPred() != null) {
                for (int j = 0; j < leftPostVersion.getPostBlocks().size(); j++) {
                    if (Objects.equals(leftPostVersion.getPostBlocks().get(j).getLocalId(), rightPostVersion.getPostBlocks().get(i).getPred().getLocalId())) {
                        leftLocalId = leftPostVersion.getPostBlocks().get(j).getLocalId();
                        break;
                    }
                }
            }

            connectionsOfTwoVersions.add(
                    new ConnectedBlocks(
                            leftLocalId,
                            rightLocalId,
                            rightPostVersion.getPostBlocks().get(i) instanceof TextBlockVersion ? 1 : 2
                    ));

        }

        return connectionsOfTwoVersions;
    }

    private ConnectionsOfTwoVersions getAllConnectionsBetweenTwoVersions_code(int leftVersionId, PostVersion leftPostVersion, PostVersion rightPostVersion) {
        ConnectionsOfTwoVersions connectionsOfTwoVersions = new ConnectionsOfTwoVersions(leftVersionId);

        for (int i = 0; i < rightPostVersion.getPostBlocks().size(); i++) {
            if (rightPostVersion.getPostBlocks().get(i) instanceof TextBlockVersion)
                continue;

            int rightLocalId = rightPostVersion.getPostBlocks().get(i).getLocalId();
            Integer leftLocalId = null;
            if (rightPostVersion.getPostBlocks().get(i).getPred() != null) {
                for (int j = 0; j < leftPostVersion.getPostBlocks().size(); j++) {
                    if (Objects.equals(leftPostVersion.getPostBlocks().get(j).getLocalId(), rightPostVersion.getPostBlocks().get(i).getPred().getLocalId())) {
                        leftLocalId = leftPostVersion.getPostBlocks().get(j).getLocalId();
                        break;
                    }
                }
            }

            connectionsOfTwoVersions.add(
                    new ConnectedBlocks(
                            leftLocalId,
                            rightLocalId,
                            rightPostVersion.getPostBlocks().get(i) instanceof TextBlockVersion ? 1 : 2
                    ));

        }

        return connectionsOfTwoVersions;
    }


    public ConnectionsOfAllVersions getAllConnectionsOfAllConsecutiveVersions_text(int postId) {
        ConnectionsOfAllVersions connectionsOfAllVersions = new ConnectionsOfAllVersions(postId);

        for (int i = 0; i < getPostVersionListWithID(postId).size() - 1; i++) {
            connectionsOfAllVersions.add(
                    getAllConnectionsBetweenTwoVersions_text(i, getPostVersionListWithID(postId).get(i), getPostVersionListWithID(postId).get(i + 1))
            );
        }

        return connectionsOfAllVersions;
    }

    public ConnectionsOfAllVersions getAllConnectionsOfAllConsecutiveVersions_code(int postId) {
        ConnectionsOfAllVersions connectionsOfAllVersions = new ConnectionsOfAllVersions(postId);

        for (int i = 0; i < getPostVersionListWithID(postId).size() - 1; i++) {
            connectionsOfAllVersions.add(
                    getAllConnectionsBetweenTwoVersions_code(i, getPostVersionListWithID(postId).get(i), getPostVersionListWithID(postId).get(i + 1))
            );
        }

        return connectionsOfAllVersions;
    }

}
