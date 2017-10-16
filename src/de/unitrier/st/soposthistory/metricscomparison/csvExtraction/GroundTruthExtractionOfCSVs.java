package de.unitrier.st.soposthistory.metricscomparison.csvExtraction;

import de.unitrier.st.soposthistory.metricscomparison.util.ConnectedBlocks;
import de.unitrier.st.soposthistory.metricscomparison.util.ConnectionsOfAllVersions;
import de.unitrier.st.soposthistory.metricscomparison.util.ConnectionsOfTwoVersions;
import de.unitrier.st.soposthistory.gt.util.BlockLifeSpanSnapshot;

import java.io.*;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

// TODO: move code needed from soposthistory.gt to package de.unitrier.st.soposthistory.lifespan?

public class GroundTruthExtractionOfCSVs {

    private LinkedList<ConnectionsOfAllVersions> groundTruth = new LinkedList<>();
    private LinkedList<ConnectionsOfAllVersions> groundTruth_text = new LinkedList<>();
    private LinkedList<ConnectionsOfAllVersions> groundTruth_code = new LinkedList<>();


    // constructor
    public GroundTruthExtractionOfCSVs(String pathOfDirectoryOfCSVs) {
        groundTruth = extractListOfConnectionsOfAllVersionsOfAllExportedCSVs(pathOfDirectoryOfCSVs);
        groundTruth.sort(Comparator.comparingInt(ConnectionsOfAllVersions::getPostId));
        divideGroundTruthIntoTextAndCode();
    }

    public LinkedList<ConnectionsOfAllVersions> getGroundTruth() {
        return groundTruth;
    }

    public LinkedList<ConnectionsOfAllVersions> getGroundTruthText() {
        return groundTruth_text;
    }

    public LinkedList<ConnectionsOfAllVersions> getGroundTruthCode() {
        return groundTruth_code;
    }

    public static LinkedList<String> parseLines(String pathToExportedCSV) {

        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new FileReader(pathToExportedCSV));
        } catch (FileNotFoundException e) {
            System.err.println("Failed to read file with path '" + pathToExportedCSV + "'.");
            System.exit(0);
        }
        LinkedList<String> lines = new LinkedList<>();

        String line;
        try {
            while ((line = bufferedReader.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            System.err.println("Failed to parse line from data at path " + pathToExportedCSV + ".");
            System.exit(0);
        }

        lines.remove(0); // first line contains header

        return lines;
    }

    private List<BlockLifeSpanSnapshot> extractBlockLifeSpanSnapshotsUnordered(List<String> lines) {

        List<BlockLifeSpanSnapshot> blockLifeSpanSnapshots = new LinkedList<>();

        for (String line : lines) {
            StringTokenizer tokens = new StringTokenizer(line, "; ");
            int postId = Integer.valueOf(tokens.nextToken().replace("\"", ""));
            int postHistoryId = Integer.valueOf(tokens.nextToken().replace("\"", ""));
            int postBlockTypeId = Integer.valueOf(tokens.nextToken().replace("\"", ""));
            int localId = Integer.valueOf(tokens.nextToken().replace("\"", ""));
            Integer predLocalId = null;
            Integer succLocalId = null;

            try {
                predLocalId = Integer.valueOf(tokens.nextToken().replace("\"", ""));
            } catch (NumberFormatException ignored) {
            }

            try {
                succLocalId = Integer.valueOf(tokens.nextToken().replace("\"", ""));
            } catch (NumberFormatException ignored) {
            }

            BlockLifeSpanSnapshot blockLifeSpanSnapshot = new BlockLifeSpanSnapshot(postId, postHistoryId, postBlockTypeId, -1, localId, predLocalId, succLocalId);
            blockLifeSpanSnapshots.add(blockLifeSpanSnapshot);
        }

        return blockLifeSpanSnapshots;
    }

    private LinkedList<LinkedList<BlockLifeSpanSnapshot>> orderBlockLifeSpanSnapshotsByPostHistoryId(List<BlockLifeSpanSnapshot> blockLifeSpanSnapshots) {
        blockLifeSpanSnapshots.sort(Comparator.comparingInt(BlockLifeSpanSnapshot::getPostHistoryId));

        int count = 1;
        LinkedList<LinkedList<BlockLifeSpanSnapshot>> listOfListOfBlockLifeSnapshotsOrderedByVersions = new LinkedList<>();

        for (BlockLifeSpanSnapshot snapshot : blockLifeSpanSnapshots) {
            if (!listOfListOfBlockLifeSnapshotsOrderedByVersions.isEmpty()
                    && listOfListOfBlockLifeSnapshotsOrderedByVersions.getLast().getLast().getPostHistoryId() == snapshot.getPostHistoryId()) {
                listOfListOfBlockLifeSnapshotsOrderedByVersions.getLast().add(snapshot);
            } else {
                listOfListOfBlockLifeSnapshotsOrderedByVersions.add(new LinkedList<>());
                listOfListOfBlockLifeSnapshotsOrderedByVersions.getLast().add(snapshot);
                count++;
            }
            listOfListOfBlockLifeSnapshotsOrderedByVersions.getLast().getLast().setVersion(count);
        }

        return listOfListOfBlockLifeSnapshotsOrderedByVersions;
    }

    private ConnectionsOfTwoVersions getAllConnectionsBetweenTwoVersions(int leftVersionId, LinkedList<BlockLifeSpanSnapshot> rightVersionOfBlocks) {
        ConnectionsOfTwoVersions connectionsOfTwoVersions = new ConnectionsOfTwoVersions(leftVersionId);
        for (int i = 0; i < rightVersionOfBlocks.size(); i++) {
            connectionsOfTwoVersions.add(
                    new ConnectedBlocks(
                            rightVersionOfBlocks.get(i).getPredLocalId(),
                            rightVersionOfBlocks.get(i).getLocalId(),
                            rightVersionOfBlocks.get(i).getPostBlockTypeId()
                    ));
        }
        return connectionsOfTwoVersions;
    }

    private ConnectionsOfAllVersions getAllConnectionsOfAllConsecutiveVersions(String pathToCSV) {
        List<String> lines = parseLines(pathToCSV);
        List<BlockLifeSpanSnapshot> listOfBlockLifeSpanSnapshots = extractBlockLifeSpanSnapshotsUnordered(lines);
        LinkedList<LinkedList<BlockLifeSpanSnapshot>> listOfListOfBlockLifeSpanSnapshots = orderBlockLifeSpanSnapshotsByPostHistoryId(listOfBlockLifeSpanSnapshots);

        ConnectionsOfAllVersions connectionsOfAllVersions = new ConnectionsOfAllVersions(listOfListOfBlockLifeSpanSnapshots.getFirst().getFirst().getPostId());

        for (int i = 1; i < listOfListOfBlockLifeSpanSnapshots.size(); i++) {
            connectionsOfAllVersions.add(
                    getAllConnectionsBetweenTwoVersions(i + 1, listOfListOfBlockLifeSpanSnapshots.get(i))
            );
        }

        return connectionsOfAllVersions;
    }

    private LinkedList<ConnectionsOfAllVersions> extractListOfConnectionsOfAllVersionsOfAllExportedCSVs(String directoryOfGroundTruthCSVs) {

        File file = new File(directoryOfGroundTruthCSVs);
        Pattern pattern = Pattern.compile("completed_" + "[0-9]+" + "\\.csv");
        File[] allCompletedPostVersionListsInFolder = file.listFiles((dir, name) -> name.matches(pattern.pattern())); // https://stackoverflow.com/questions/4852531/find-files-in-a-folder-using-java

        assert allCompletedPostVersionListsInFolder != null;
        for (File completedCSV : allCompletedPostVersionListsInFolder) {
            try {
                groundTruth.add(getAllConnectionsOfAllConsecutiveVersions(completedCSV.getCanonicalPath()));
            } catch (IOException e) {
                System.err.println("Failed to read canonical path of data '" + completedCSV.getName() + "'.");
                System.exit(0);
            }
        }

        //groundTruth.sort(Comparator.comparingInt(o -> o.getFirst().getFirst()));

        return groundTruth;
    }

    private void divideGroundTruthIntoTextAndCode() {
        for (ConnectionsOfAllVersions allVersionsOfConnections : groundTruth) {
            groundTruth_text.add(new ConnectionsOfAllVersions(allVersionsOfConnections.getPostId()));
            groundTruth_code.add(new ConnectionsOfAllVersions(allVersionsOfConnections.getPostId()));
            int count = 1;
            for (ConnectionsOfTwoVersions twoVersionsOfConnections : allVersionsOfConnections) {
                groundTruth_text.getLast().add(new ConnectionsOfTwoVersions(count));
                groundTruth_code.getLast().add(new ConnectionsOfTwoVersions(count));
                for (ConnectedBlocks connectedBlock : twoVersionsOfConnections) {
                    if (connectedBlock.getPostBlockTypeId() == 1) {
                        groundTruth_text.getLast().getLast().add(connectedBlock);
                    } else {
                        groundTruth_code.getLast().getLast().add(connectedBlock);
                    }
                }
                count++;
            }
        }
    }


    public ConnectionsOfAllVersions getAllConnectionsOfAllConsecutiveVersions_text(int postId) {
        for (ConnectionsOfAllVersions groundTruth_text : groundTruth_text)
            if (groundTruth_text.getPostId() == postId)
                return groundTruth_text;

        return null;
    }

    public ConnectionsOfAllVersions getAllConnectionsOfAllConsecutiveVersions_code(int postId) {
        for (ConnectionsOfAllVersions groundTruth_code : groundTruth_code)
            if (groundTruth_code.getPostId() == postId)
                return groundTruth_code;

        return null;
    }
}
