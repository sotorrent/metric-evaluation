package de.unitrier.st.soposthistory.metricscomparison.statistics;

import de.unitrier.st.soposthistory.blocks.CodeBlockVersion;
import de.unitrier.st.soposthistory.blocks.PostBlockVersion;
import de.unitrier.st.soposthistory.blocks.TextBlockVersion;
import de.unitrier.st.soposthistory.metricscomparison.csvExtraction.PostVersionsListManagement;
import de.unitrier.st.soposthistory.version.PostVersion;
import de.unitrier.st.soposthistory.version.PostVersionList;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

public class Statistics {

    LinkedList<String> pathToAllDirectories = new LinkedList<>();


    Statistics(){

        pathToAllDirectories.add("testdata\\representative CSVs");
/*
        pathToAllDirectories.add(Paths.get("testdata", "PostId_VersionCount_SO_17-06_sample_10000_1", "files").toString());
        pathToAllDirectories.add(Paths.get("testdata", "PostId_VersionCount_SO_17-06_sample_10000_2", "files").toString());
        pathToAllDirectories.add(Paths.get("testdata", "PostId_VersionCount_SO_17-06_sample_10000_3", "files").toString());
        pathToAllDirectories.add(Paths.get("testdata", "PostId_VersionCount_SO_17-06_sample_10000_4", "files").toString());
        pathToAllDirectories.add(Paths.get("testdata", "PostId_VersionCount_SO_17-06_sample_10000_5", "files").toString());
        pathToAllDirectories.add(Paths.get("testdata", "PostId_VersionCount_SO_17-06_sample_10000_6", "files").toString());
        pathToAllDirectories.add(Paths.get("testdata", "PostId_VersionCount_SO_17-06_sample_10000_7", "files").toString());
        pathToAllDirectories.add(Paths.get("testdata", "PostId_VersionCount_SO_17-06_sample_10000_8", "files").toString());
        pathToAllDirectories.add(Paths.get("testdata", "PostId_VersionCount_SO_17-06_sample_10000_9", "files").toString());
        pathToAllDirectories.add(Paths.get("testdata", "PostId_VersionCount_SO_17-06_sample_10000_10", "files").toString());
                */
    }


    public static void main(String[] args){

        // TODO: find more ideas for other statistics?

        Statistics statistics = new Statistics();

        statistics.findPostsWithPossibleMultipleConnections();
        statistics.testFindPostVersionListsWithEmptyBlocks();
        statistics.getStatisticsOfBlockSizes();
        statistics.getStatisticsOfBlocksWithMultipleLinkingPossibilities();
        statistics.getAverageSizesOfBlocksAndVersions();
        statistics.getPostsWithMultipleChoicesForBlocksAndFirstChoiceIsNotTheRightOne();
    }


    private void findPostsWithPossibleMultipleConnections() {

        StringBuilder output = new StringBuilder();

        output.append("post-id; post-history-id; local-id; blockTypeId; possible pred or succ local-ids; number of possible successors or predecessors\n");

        for (String path : pathToAllDirectories) {
            PostVersionsListManagement postVersionsListManagement = new PostVersionsListManagement(path);
            for (PostVersionList postVersionList : postVersionsListManagement.postVersionLists) {
                for (int j = 0; j < postVersionList.size(); j++) {
                    if (j > 0) {
                        for (int k = 0; k < postVersionList.get(j).getPostBlocks().size(); k++) {
                            PostBlockVersion postBlockVersion = postVersionList.get(j).getPostBlocks().get(k);
                            LinkedList<Integer> possiblePreds = new LinkedList<>();
                            for (int l = 0; l < postVersionList.get(j - 1).getPostBlocks().size(); l++) {
                                PostBlockVersion postBlockVersionPred = postVersionList.get(j - 1).getPostBlocks().get(l);

                                if (postBlockVersion.getContent().equals(postBlockVersionPred.getContent()))
                                    possiblePreds.add(postBlockVersionPred.getLocalId());
                            }

                            if (possiblePreds.size() > 1) {
                                output
                                        .append(postVersionList.getFirst().getPostId())
                                        .append("; ")
                                        .append(postVersionList.get(j).getPostHistoryId())
                                        .append("; ")
                                        .append(postVersionList.get(j).getPostBlocks().get(k).getLocalId())
                                        .append("; ")
                                        .append(postVersionList.get(j).getPostBlocks().get(k) instanceof TextBlockVersion ? 1 : 2)
                                        .append("; ")
                                        .append("local-ids of possible preds: ")
                                        .append(possiblePreds)
                                        .append("; ")
                                        .append(possiblePreds.size())
                                        .append("\n");
                            }
                        }
                    }

                    if (j < postVersionList.size() - 1) {
                        for (int k = 0; k < postVersionList.get(j).getPostBlocks().size(); k++) {
                            PostBlockVersion postBlockVersion = postVersionList.get(j).getPostBlocks().get(k);
                            LinkedList<Integer> possibleSuccs = new LinkedList<>();
                            for (int l = 0; l < postVersionList.get(j + 1).getPostBlocks().size(); l++) {
                                PostBlockVersion postBlockVersionSucc = postVersionList.get(j + 1).getPostBlocks().get(l);

                                if (postBlockVersion.getContent().equals(postBlockVersionSucc.getContent()))
                                    possibleSuccs.add(postBlockVersionSucc.getLocalId());
                            }

                            if (possibleSuccs.size() > 1) {
                                output
                                        .append(postVersionList.getFirst().getPostId())
                                        .append("; ")
                                        .append(postVersionList.get(j).getPostHistoryId())
                                        .append("; ")
                                        .append(postVersionList.get(j).getPostBlocks().get(k).getLocalId())
                                        .append("; ")
                                        .append(postVersionList.get(j).getPostBlocks().get(k) instanceof TextBlockVersion ? 1 : 2)
                                        .append("; ")
                                        .append("local-ids of possible succs: ")
                                        .append(possibleSuccs)
                                        .append("; ")
                                        .append(possibleSuccs.size())
                                        .append("\n");
                            }
                        }
                    }
                }
            }

            System.out.println("Finished: " + path);
        }

        System.out.println(output);
    }

    private void testFindPostVersionListsWithEmptyBlocks() {
        ArrayList<Integer> postIdsEmptyBlocks = new ArrayList<>();


        for (String path : pathToAllDirectories) {
            PostVersionsListManagement postVersionsListManagement = new PostVersionsListManagement(path);
            for (PostVersionList postVersionList : postVersionsListManagement.postVersionLists) {
                for (PostVersion postVersion : postVersionList) {
                    for (int k = 0; k < postVersion.getPostBlocks().size(); k++) {
                        if (Pattern.matches("\\s*", postVersion.getPostBlocks().get(k).getContent())) {
                            if (!postIdsEmptyBlocks.contains(postVersion.getPostId()))
                                postIdsEmptyBlocks.add(postVersion.getPostId());
                            break;
                        }
                    }
                }
            }
            System.out.println(postIdsEmptyBlocks);
        }
    }

    private void getStatisticsOfBlockSizes() {

        LinkedList<Integer> distinctValuesOfBlockLenghtsText = new LinkedList<>();
        HashMap<Integer, Integer> frequenciesOfBlockLenghtsText = new HashMap<>();
        LinkedList<Integer> affectedPostHistoriesText = new LinkedList<>();
        LinkedList<Integer> affectedPostsText = new LinkedList<>();

        LinkedList<Integer> distinctValuesOfBlockLenghtsCode = new LinkedList<>();
        HashMap<Integer, Integer> frequenciesOfBlockLenghtsCode = new HashMap<>();
        LinkedList<Integer> affectedPostHistoriesCode = new LinkedList<>();
        LinkedList<Integer> affectedPostsCode = new LinkedList<>();

        for (String path : pathToAllDirectories) {
            PostVersionsListManagement postVersionsListManagement = new PostVersionsListManagement(path);
            for (PostVersionList postVersionList : postVersionsListManagement.postVersionLists) {
                for (PostVersion postVersion : postVersionList) {
                    for (PostBlockVersion postBlockVersion : postVersion.getPostBlocks()) {

                        int blockLength = postBlockVersion.getContent().length();

                        if (blockLength <= 5) {
                            if (postBlockVersion instanceof TextBlockVersion) {
                                if (!distinctValuesOfBlockLenghtsText.contains(blockLength)) {
                                    distinctValuesOfBlockLenghtsText.add(blockLength);
                                    frequenciesOfBlockLenghtsText.put(blockLength, 1);
                                } else {
                                    frequenciesOfBlockLenghtsText.replace(
                                            blockLength,
                                            frequenciesOfBlockLenghtsText.get(blockLength),
                                            frequenciesOfBlockLenghtsText.get(blockLength) + 1
                                    );
                                }

                                if (!affectedPostHistoriesText.contains(postVersion.getPostHistoryId()))
                                    affectedPostHistoriesText.add(postVersion.getPostHistoryId());

                                if (!affectedPostsText.contains(postVersion.getPostId()))
                                    affectedPostsText.add(postVersion.getPostId());
                            } else if (postBlockVersion instanceof CodeBlockVersion) {
                                if (!distinctValuesOfBlockLenghtsCode.contains(blockLength)) {
                                    distinctValuesOfBlockLenghtsCode.add(blockLength);
                                    frequenciesOfBlockLenghtsCode.put(blockLength, 1);
                                } else {
                                    frequenciesOfBlockLenghtsCode.replace(
                                            blockLength,
                                            frequenciesOfBlockLenghtsCode.get(blockLength),
                                            frequenciesOfBlockLenghtsCode.get(blockLength) + 1
                                    );
                                }

                                if (!affectedPostHistoriesCode.contains(postVersion.getPostHistoryId()))
                                    affectedPostHistoriesCode.add(postVersion.getPostHistoryId());

                                if (!affectedPostsCode.contains(postVersion.getPostId()))
                                    affectedPostsCode.add(postVersion.getPostId());
                            }
                        }

                    }
                }
            }

            System.out.println("completed: " + path);
        }

        System.out.println("Text:");
        System.out.println("blockLength; frequencies");
        for (Integer value : distinctValuesOfBlockLenghtsText) {
            System.out.println(value + "; " + frequenciesOfBlockLenghtsText.get(value));
        }
        System.out.println();

        System.out.println("Code:");
        System.out.println("blockLength; frequencies");
        for (Integer value : distinctValuesOfBlockLenghtsCode) {
            System.out.println(value + "; " + frequenciesOfBlockLenghtsCode.get(value));
        }
        System.out.println();

        System.out.println("number of affected post histories for text: " + affectedPostHistoriesText.size());
        System.out.println("number of affected post histories for code: " + affectedPostHistoriesCode.size());
        System.out.println();

        System.out.println("number of affected posts for text: " + affectedPostsText.size());
        System.out.println("number of affected posts for code: " + affectedPostsCode.size());
    }

    private void getStatisticsOfBlocksWithMultipleLinkingPossibilities() {
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(Paths.get("testdata", "statistics", "possible multiple connections.csv").toString()));

            LinkedList<Integer> postIdWithMultipleChoices = new LinkedList<>();

            LinkedList<Integer> distinctValuesOfPossibleBlockTypeIds = new LinkedList<>();
            HashMap<Integer, Integer> frequenciesOfBlocKTypeIds = new HashMap<>();

            LinkedList<Integer> distinctValuesOfPossiblePredBlocks = new LinkedList<>();
            HashMap<Integer, Integer> frequenciesOfPossiblePredBlocks = new HashMap<>();

            LinkedList<Integer> distinctValuesOfPossiblePredPostHistoryIds = new LinkedList<>();
            HashMap<Integer, Integer> frequenciesOfAffectedPredPostHistoryIds = new HashMap<>();

            LinkedList<Integer> distinctValuesOfPossibleSuccBlocks = new LinkedList<>();
            HashMap<Integer, Integer> frequenciesOfPossibleSuccBlocks = new HashMap<>();

            LinkedList<Integer> distinctValuesOfPossibleSuccPostHistoryIds = new LinkedList<>();
            HashMap<Integer, Integer> frequenciesOfAffectedSuccPostHistoryIds = new HashMap<>();


            boolean firstLine = true;
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue;
                }
                StringTokenizer tokens = new StringTokenizer(line, ";");

                Integer postId = Integer.valueOf(tokens.nextToken());
                Integer postHistoryId = Integer.valueOf(tokens.nextToken().trim());
                Integer localId = Integer.valueOf(tokens.nextToken().trim());
                Integer blockTypeId = Integer.valueOf(tokens.nextToken().trim());

                String predOrSuccs = tokens.nextToken();
                Boolean hasPossiblePreds = predOrSuccs.contains("pred");
                Boolean hasPossibleSuccs = predOrSuccs.contains("succ");
                String localIds = predOrSuccs.substring(28);
                Integer numberOfPossibleLinks = Integer.valueOf(tokens.nextToken().trim());

                if (hasPossiblePreds) {
                    if (frequenciesOfBlocKTypeIds.get(blockTypeId) == null && !distinctValuesOfPossibleBlockTypeIds.contains(blockTypeId)) {
                        distinctValuesOfPossibleBlockTypeIds.add(blockTypeId);
                        frequenciesOfBlocKTypeIds.put(blockTypeId, 1);
                    } else {
                        frequenciesOfBlocKTypeIds.replace(blockTypeId, frequenciesOfBlocKTypeIds.get(blockTypeId), frequenciesOfBlocKTypeIds.get(blockTypeId) + 1);
                    }

                    if (frequenciesOfPossiblePredBlocks.get(numberOfPossibleLinks) == null && !distinctValuesOfPossiblePredBlocks.contains(numberOfPossibleLinks)) {
                        distinctValuesOfPossiblePredBlocks.add(numberOfPossibleLinks);
                        frequenciesOfPossiblePredBlocks.put(numberOfPossibleLinks, 1);
                    } else {
                        frequenciesOfPossiblePredBlocks.replace(numberOfPossibleLinks, frequenciesOfPossiblePredBlocks.get(numberOfPossibleLinks), frequenciesOfPossiblePredBlocks.get(numberOfPossibleLinks) + 1);
                    }

                    if (!distinctValuesOfPossiblePredPostHistoryIds.contains(postHistoryId)) {
                        distinctValuesOfPossiblePredPostHistoryIds.add(postHistoryId);

                        if (frequenciesOfAffectedPredPostHistoryIds.get(numberOfPossibleLinks) == null) {
                            frequenciesOfAffectedPredPostHistoryIds.put(numberOfPossibleLinks, 1);
                        } else {
                            frequenciesOfAffectedPredPostHistoryIds.replace(numberOfPossibleLinks, frequenciesOfAffectedPredPostHistoryIds.get(numberOfPossibleLinks), frequenciesOfAffectedPredPostHistoryIds.get(numberOfPossibleLinks) + 1);
                        }
                    }
                }

                if (hasPossibleSuccs) {
                    if (frequenciesOfBlocKTypeIds.get(blockTypeId) == null && !distinctValuesOfPossibleBlockTypeIds.contains(blockTypeId)) {
                        distinctValuesOfPossibleBlockTypeIds.add(blockTypeId);
                        frequenciesOfBlocKTypeIds.put(blockTypeId, 1);
                    } else {
                        frequenciesOfBlocKTypeIds.replace(blockTypeId, frequenciesOfBlocKTypeIds.get(blockTypeId), frequenciesOfBlocKTypeIds.get(blockTypeId) + 1);
                    }

                    if (frequenciesOfPossibleSuccBlocks.get(numberOfPossibleLinks) == null && !distinctValuesOfPossibleSuccBlocks.contains(numberOfPossibleLinks)) {
                        distinctValuesOfPossibleSuccBlocks.add(numberOfPossibleLinks);
                        frequenciesOfPossibleSuccBlocks.put(numberOfPossibleLinks, 1);
                    } else {
                        frequenciesOfPossibleSuccBlocks.replace(numberOfPossibleLinks, frequenciesOfPossibleSuccBlocks.get(numberOfPossibleLinks), frequenciesOfPossibleSuccBlocks.get(numberOfPossibleLinks) + 1);
                    }

                    if (!distinctValuesOfPossibleSuccPostHistoryIds.contains(postHistoryId)) {
                        distinctValuesOfPossibleSuccPostHistoryIds.add(postHistoryId);

                        if (frequenciesOfAffectedSuccPostHistoryIds.get(numberOfPossibleLinks) == null) {
                            frequenciesOfAffectedSuccPostHistoryIds.put(numberOfPossibleLinks, 1);
                        } else {
                            frequenciesOfAffectedSuccPostHistoryIds.replace(numberOfPossibleLinks, frequenciesOfAffectedSuccPostHistoryIds.get(numberOfPossibleLinks), frequenciesOfAffectedSuccPostHistoryIds.get(numberOfPossibleLinks) + 1);
                        }
                    }
                }

                if (!postIdWithMultipleChoices.contains(postId))
                    postIdWithMultipleChoices.add(postId);
            }

            System.out.println("number of post ids that contain blocks which could be connected to multiple predecessors or successors: " + postIdWithMultipleChoices.size() + " of 100000 (" + ((double) postIdWithMultipleChoices.size() / 100000) * 100 + " %)");
            System.out.println();

            System.out.println("Distinct numbers of predecessor blocks that could be linked; possible predecessor blocks that could be matched; number of predecessor postHistories");
            Collections.sort(distinctValuesOfPossiblePredBlocks);
            for (Integer value : distinctValuesOfPossiblePredBlocks) {
                System.out.println(value + "; " + frequenciesOfPossiblePredBlocks.get(value) + "; " + frequenciesOfAffectedPredPostHistoryIds.get(value));
            }
            System.out.println();

            System.out.println("Distinct numbers of successor blocks that could be linked; possible successor blocks that could be matched; number of successor postHistories");
            Collections.sort(distinctValuesOfPossibleSuccBlocks);
            for (Integer value : distinctValuesOfPossibleSuccBlocks) {
                System.out.println(value + "; " + frequenciesOfPossibleSuccBlocks.get(value) + "; " + frequenciesOfAffectedSuccPostHistoryIds.get(value));
            }

            System.out.println();
            System.out.println("Affected text blocks: " + frequenciesOfBlocKTypeIds.get(1));
            System.out.println("Affected code blocks: " + frequenciesOfBlocKTypeIds.get(2));


        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void getAverageSizesOfBlocksAndVersions() {

        long textBlockLength = 0;
        int numberOfTextBlocks = 0;

        long codeBlockLength = 0;
        int numberOfCodeBlocks = 0;

        int numberOfVersions = 0;

        int numberOfPosts = 0;

        for (String path : pathToAllDirectories) {
            PostVersionsListManagement postVersionsListManagement = new PostVersionsListManagement(path);
            for (PostVersionList postVersionList : postVersionsListManagement.postVersionLists) {

                numberOfPosts++;

                for (PostVersion postVersion : postVersionList) {
                    for (int k = 0; k < postVersion.getTextBlocks().size(); k++) {
                        textBlockLength += postVersion.getTextBlocks().get(k).getContent().length();
                    }
                    for (int k = 0; k < postVersion.getCodeBlocks().size(); k++) {
                        codeBlockLength += postVersion.getCodeBlocks().get(k).getContent().length();
                    }

                    numberOfTextBlocks += postVersion.getTextBlocks().size();
                    numberOfCodeBlocks += postVersion.getCodeBlocks().size();
                }

                numberOfVersions += postVersionList.size();
            }

            System.out.println("Finished: " + path);
        }

        System.out.println("number of versions: " + numberOfVersions);
        System.out.println("average number of versions: " + (double) numberOfVersions / numberOfPosts);
        System.out.println();
        System.out.println("number of text blocks: " + numberOfTextBlocks);
        System.out.println("average number of text blocks: " + (double) numberOfTextBlocks / numberOfVersions);
        System.out.println("number of code blocks: " + numberOfCodeBlocks);
        System.out.println("average number of code blocks: " + (double) numberOfCodeBlocks / numberOfVersions);
        System.out.println();
        System.out.println("text block length: " + textBlockLength);
        System.out.println("average text block length: " + (double) textBlockLength / numberOfTextBlocks);
        System.out.println("code block length: " + codeBlockLength);
        System.out.println("average code block length: " + (double) codeBlockLength / numberOfCodeBlocks);
    }

    private void getPostsWithMultipleChoicesForBlocksAndFirstChoiceIsNotTheRightOne(){

        TextBlockVersion.similarityMetric = de.unitrier.st.stringsimilarity.edit.Variants::levenshtein; // Levenshtein does not need a minimum size length so it has been chosen
        TextBlockVersion.similarityThreshold = 1; // Conclusions for blocks that matches with a similarity of 1.0 can be assigned to a lower threshold more easily

        CodeBlockVersion.similarityMetric = de.unitrier.st.stringsimilarity.edit.Variants::levenshtein; // Levenshtein does not need a minimum size length so it has been chosen
        CodeBlockVersion.similarityThreshold = 1; // Conclusions for blocks that matches with a similarity of 1.0 can be assigned to a lower threshold more easily

        PostVersionsListManagement postVersionsListManagement = new PostVersionsListManagement(
                FileSystems.getDefault().getPath("Metrics comparison", "testdata", "PostId_VersionCount_SO_17-06_sample_500_multiple_possible_links").toString());

        List<String> suspectedPostsList = new ArrayList<>();

        for(PostVersionList postVersionList : postVersionsListManagement.postVersionLists){
            postVersionList.processVersionHistory();

            for(int i=1; i<postVersionList.size(); i++){
                // collect equal block pairs
                List<EqualBlockPairs> clonePairs = new ArrayList<>();
                for(int k=0; k<postVersionList.get(i).getPostBlocks().size(); k++){
                    for(int j=0; j<postVersionList.get(i-1).getPostBlocks().size(); j++){
                        PostBlockVersion blockLeft = postVersionList.get(i-1).getPostBlocks().get(j);
                        PostBlockVersion blockRight = postVersionList.get(i).getPostBlocks().get(k);

                        if(blockLeft instanceof TextBlockVersion != blockRight instanceof TextBlockVersion)
                            continue;

                        if(blockLeft.getContent().equals(blockRight.getContent())){
                            boolean clonePairsAlreadyExists = false;
                            int position = 0;
                            for(int l=0; l<clonePairs.size(); l++){
                                if(clonePairs.get(l).content.equals(blockLeft.getContent())){
                                    clonePairsAlreadyExists = true;
                                    position = l;
                                    break;
                                }
                            }
                            if(!clonePairsAlreadyExists){
                                clonePairs.add(new EqualBlockPairs(blockLeft.getContent()));
                                position = clonePairs.size()-1;
                            }

                            if(!clonePairs.get(position).localIdsLeft.contains(blockLeft.getLocalId()))
                                clonePairs.get(position).localIdsLeft.add(blockLeft.getLocalId());

                            if(!clonePairs.get(position).localIdsRight.contains(blockRight.getLocalId()))
                                clonePairs.get(position).localIdsRight.add(blockRight.getLocalId());
                        }
                    }
                }

                // delete pairs with only one possible link from tmp list
                for(int j=clonePairs.size()-1; j>=0; j--) {
                    if (clonePairs.get(j).localIdsLeft.size() == 1 && clonePairs.get(j).localIdsRight.size() == 1) {
                        clonePairs.remove(j);
                    }
                }

                // delete pairs with equal positions of matches
                for(int j=clonePairs.size()-1; j>=0; j--) {
                    boolean remove = false;
                    if(clonePairs.get(j).localIdsLeft.equals(clonePairs.get(j).localIdsRight)){
                        remove = true;
                        for(int k=0; k<clonePairs.get(j).localIdsRight.size(); k++){

                            for(int l=0; l<postVersionList.get(i).getPostBlocks().size(); l++){
                                if(Objects.equals(postVersionList.get(i).getPostBlocks().get(l).getLocalId(), clonePairs.get(j).localIdsRight.get(k))){
                                    if(postVersionList.get(i).getPostBlocks().get(l).getPred() != null){
                                        int leftId = postVersionList.get(i).getPostBlocks().get(l).getPred().getLocalId();
                                        int rightId = postVersionList.get(i).getPostBlocks().get(l).getLocalId();

                                        if(leftId != rightId){
                                            remove = false;
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if(remove)
                        clonePairs.remove(j);
                }

                // check whether first choice has not been set but another
                List<String> connections = new ArrayList<>();
                for(int j=clonePairs.size()-1; j>=0; j--){
                    List<Integer> remainingLocalIdsLeft = new ArrayList<>(clonePairs.get(j).localIdsLeft);
                    for(int k=0; k<clonePairs.get(j).localIdsRight.size(); k++){
                        Integer leftId = null;

                        for(int l=0; l<postVersionList.get(i).getPostBlocks().size(); l++){
                            if(Objects.equals(postVersionList.get(i).getPostBlocks().get(l).getLocalId(), clonePairs.get(j).localIdsRight.get(k))){
                                if(postVersionList.get(i).getPostBlocks().get(l).getPred() != null){
                                    leftId = postVersionList.get(i).getPostBlocks().get(l).getPred().getLocalId();
                                }
                            }
                        }
                        connections.add(leftId + " <- " + clonePairs.get(j).localIdsRight.get(k));
                        remainingLocalIdsLeft.remove(leftId);
                    }

                    for (Integer leftLocalId : remainingLocalIdsLeft) {
                        connections.add(leftLocalId + " <- " + null);
                    }
                }

                connections.sort((o1, o2) -> {
                    StringTokenizer connection1 = new StringTokenizer(o1, " <-");
                    StringTokenizer connection2 = new StringTokenizer(o2, " <-");

                    Integer c_1_1 = null;
                    Integer c_1_2 = null;
                    Integer c_2_1 = null;
                    Integer c_2_2 = null;

                    try {
                        c_1_1 = Integer.valueOf(connection1.nextToken());
                    }catch(NumberFormatException ignored){}

                    try {
                        c_1_2 = Integer.valueOf(connection1.nextToken());
                    }catch(NumberFormatException ignored){}

                    try {
                        c_2_1 = Integer.valueOf(connection2.nextToken());
                    }catch(NumberFormatException ignored){}

                    try {
                        c_2_2 = Integer.valueOf(connection2.nextToken());
                    }catch(NumberFormatException ignored){}


                    return (c_1_1 != null ? c_1_1 : c_1_2) - (c_2_1 != null ? c_2_1 : c_2_2);
                });

                boolean reachedNull = false;
                for (String connection : connections) {
                    if (!reachedNull && connection.matches(".*null.*")) {
                        reachedNull = true;
                    } else if (reachedNull && !connection.matches(".*null.*")) {
                        suspectedPostsList.add(postVersionList.getFirst().getPostId() + ":" + i + ": (" + connection + ")" + "\n");

                        /*
                        System.out.println(postVersionList.getFirst().getPostId());

                        if(!connections.isEmpty())
                            System.out.println(connections);
                        for(int j=0; j<clonePairs.size(); j++){
                            System.out.println(postVersionList.getFirst().getPostId() + ":" + (i) + ": " + clonePairs.get(j).localIdsLeft + " : " + clonePairs.get(j).localIdsRight);
                            System.out.println();
                        }
                        */

                        break;
                    }
                }
            }
        }

        Collections.sort(suspectedPostsList);
        System.out.println(suspectedPostsList);
    }

}
