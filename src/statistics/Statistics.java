package statistics;

import csvExtraction.PostVersionsListManagement;
import de.unitrier.st.soposthistory.blocks.CodeBlockVersion;
import de.unitrier.st.soposthistory.blocks.PostBlockVersion;
import de.unitrier.st.soposthistory.blocks.TextBlockVersion;
import de.unitrier.st.soposthistory.version.PostVersion;
import de.unitrier.st.soposthistory.version.PostVersionList;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

public class Statistics {

    // TODO: find more ideas for statistics

    @Test
    public void testPostVersionListManagement(){
        LinkedList<String> pathToAllDirectories = new LinkedList<>();

        /*
        pathToAllDirectories.add("testdata\\representative CSVs");
        */
        pathToAllDirectories.add("C:\\Users\\Lorik\\Desktop\\5. Semester\\Master-Arbeit\\PostVersionLists\\PostId_VersionCount_SO_17-06_sample_10000_1\\files");
        pathToAllDirectories.add("C:\\Users\\Lorik\\Desktop\\5. Semester\\Master-Arbeit\\PostVersionLists\\PostId_VersionCount_SO_17-06_sample_10000_2\\files");
        pathToAllDirectories.add("C:\\Users\\Lorik\\Desktop\\5. Semester\\Master-Arbeit\\PostVersionLists\\PostId_VersionCount_SO_17-06_sample_10000_3\\files");
        pathToAllDirectories.add("C:\\Users\\Lorik\\Desktop\\5. Semester\\Master-Arbeit\\PostVersionLists\\PostId_VersionCount_SO_17-06_sample_10000_4\\files");
        pathToAllDirectories.add("C:\\Users\\Lorik\\Desktop\\5. Semester\\Master-Arbeit\\PostVersionLists\\PostId_VersionCount_SO_17-06_sample_10000_5\\files");
        pathToAllDirectories.add("C:\\Users\\Lorik\\Desktop\\5. Semester\\Master-Arbeit\\PostVersionLists\\PostId_VersionCount_SO_17-06_sample_10000_6\\files");
        pathToAllDirectories.add("C:\\Users\\Lorik\\Desktop\\5. Semester\\Master-Arbeit\\PostVersionLists\\PostId_VersionCount_SO_17-06_sample_10000_7\\files");
        pathToAllDirectories.add("C:\\Users\\Lorik\\Desktop\\5. Semester\\Master-Arbeit\\PostVersionLists\\PostId_VersionCount_SO_17-06_sample_10000_8\\files");
        pathToAllDirectories.add("C:\\Users\\Lorik\\Desktop\\5. Semester\\Master-Arbeit\\PostVersionLists\\PostId_VersionCount_SO_17-06_sample_10000_9\\files");
        pathToAllDirectories.add("C:\\Users\\Lorik\\Desktop\\5. Semester\\Master-Arbeit\\PostVersionLists\\PostId_VersionCount_SO_17-06_sample_10000_10\\files");


        StringBuilder output = new StringBuilder();

        output.append("post-id; post-history-id; local-id; blockTypeId; possible pred or succ local-ids; number of possible successors or predecessors\n");

        for(String path : pathToAllDirectories) {
            PostVersionsListManagement postVersionsListManagement = new PostVersionsListManagement(path);
            for(PostVersionList postVersionList : postVersionsListManagement.postVersionLists) {
                for (int j=0; j<postVersionList.size(); j++) {
                    if(j > 0) {
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

                    if(j < postVersionList.size()-1){
                        for (int k = 0; k < postVersionList.get(j).getPostBlocks().size(); k++) {
                            PostBlockVersion postBlockVersion = postVersionList.get(j).getPostBlocks().get(k);
                            LinkedList<Integer> possibleSuccs = new LinkedList<>();
                            for (int l = 0; l < postVersionList.get(j+1).getPostBlocks().size(); l++) {
                                PostBlockVersion postBlockVersionSucc = postVersionList.get(j+1).getPostBlocks().get(l);

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

    @Test
    public void testFindPostVersionListsWithEmptyBlocks(){
        LinkedList<String> pathToAllDirectories = new LinkedList<>();

        pathToAllDirectories.add("testdata\\representative CSVs");
        /*
        pathToAllDirectories.add("C:\\Users\\Lorik\\Desktop\\5. Semester\\Master-Arbeit\\PostVersionLists\\PostId_VersionCount_SO_17-06_sample_10000_1\\files");
        pathToAllDirectories.add("C:\\Users\\Lorik\\Desktop\\5. Semester\\Master-Arbeit\\PostVersionLists\\PostId_VersionCount_SO_17-06_sample_10000_2\\files");
        pathToAllDirectories.add("C:\\Users\\Lorik\\Desktop\\5. Semester\\Master-Arbeit\\PostVersionLists\\PostId_VersionCount_SO_17-06_sample_10000_3\\files");
        pathToAllDirectories.add("C:\\Users\\Lorik\\Desktop\\5. Semester\\Master-Arbeit\\PostVersionLists\\PostId_VersionCount_SO_17-06_sample_10000_4\\files");
        pathToAllDirectories.add("C:\\Users\\Lorik\\Desktop\\5. Semester\\Master-Arbeit\\PostVersionLists\\PostId_VersionCount_SO_17-06_sample_10000_5\\files");
        pathToAllDirectories.add("C:\\Users\\Lorik\\Desktop\\5. Semester\\Master-Arbeit\\PostVersionLists\\PostId_VersionCount_SO_17-06_sample_10000_6\\files");
        pathToAllDirectories.add("C:\\Users\\Lorik\\Desktop\\5. Semester\\Master-Arbeit\\PostVersionLists\\PostId_VersionCount_SO_17-06_sample_10000_7\\files");
        pathToAllDirectories.add("C:\\Users\\Lorik\\Desktop\\5. Semester\\Master-Arbeit\\PostVersionLists\\PostId_VersionCount_SO_17-06_sample_10000_8\\files");
        pathToAllDirectories.add("C:\\Users\\Lorik\\Desktop\\5. Semester\\Master-Arbeit\\PostVersionLists\\PostId_VersionCount_SO_17-06_sample_10000_9\\files");
        pathToAllDirectories.add("C:\\Users\\Lorik\\Desktop\\5. Semester\\Master-Arbeit\\PostVersionLists\\PostId_VersionCount_SO_17-06_sample_10000_10\\files");
*/
        ArrayList<Integer> postIdsEmptyBlocks = new ArrayList<>();


        for(String path : pathToAllDirectories) {
            PostVersionsListManagement postVersionsListManagement = new PostVersionsListManagement(path);
            for(PostVersionList postVersionList : postVersionsListManagement.postVersionLists) {
                for (PostVersion postVersion : postVersionList) {
                    for(int k=0; k<postVersion.getPostBlocks().size(); k++) {
                        if (Pattern.matches("\\s*", postVersion.getPostBlocks().get(k).getContent())){
                            if(!postIdsEmptyBlocks.contains(postVersion.getPostId()))
                                postIdsEmptyBlocks.add(postVersion.getPostId());
                            break;
                        }
                    }
                }
            }
            System.out.println(postIdsEmptyBlocks);
        }
    }

    @Test
    public void getStatisticsOfBlockSizes(){
        LinkedList<String> pathToAllDirectories = new LinkedList<>();

        // pathToAllDirectories.add("testdata\\representative CSVs");
        pathToAllDirectories.add("C:\\Users\\Lorik\\Desktop\\5. Semester\\Master-Arbeit\\PostVersionLists\\PostId_VersionCount_SO_17-06_sample_10000_1\\files");
        pathToAllDirectories.add("C:\\Users\\Lorik\\Desktop\\5. Semester\\Master-Arbeit\\PostVersionLists\\PostId_VersionCount_SO_17-06_sample_10000_2\\files");
        pathToAllDirectories.add("C:\\Users\\Lorik\\Desktop\\5. Semester\\Master-Arbeit\\PostVersionLists\\PostId_VersionCount_SO_17-06_sample_10000_3\\files");
        pathToAllDirectories.add("C:\\Users\\Lorik\\Desktop\\5. Semester\\Master-Arbeit\\PostVersionLists\\PostId_VersionCount_SO_17-06_sample_10000_4\\files");
        pathToAllDirectories.add("C:\\Users\\Lorik\\Desktop\\5. Semester\\Master-Arbeit\\PostVersionLists\\PostId_VersionCount_SO_17-06_sample_10000_5\\files");
        pathToAllDirectories.add("C:\\Users\\Lorik\\Desktop\\5. Semester\\Master-Arbeit\\PostVersionLists\\PostId_VersionCount_SO_17-06_sample_10000_6\\files");
        pathToAllDirectories.add("C:\\Users\\Lorik\\Desktop\\5. Semester\\Master-Arbeit\\PostVersionLists\\PostId_VersionCount_SO_17-06_sample_10000_7\\files");
        pathToAllDirectories.add("C:\\Users\\Lorik\\Desktop\\5. Semester\\Master-Arbeit\\PostVersionLists\\PostId_VersionCount_SO_17-06_sample_10000_8\\files");
        pathToAllDirectories.add("C:\\Users\\Lorik\\Desktop\\5. Semester\\Master-Arbeit\\PostVersionLists\\PostId_VersionCount_SO_17-06_sample_10000_9\\files");
        pathToAllDirectories.add("C:\\Users\\Lorik\\Desktop\\5. Semester\\Master-Arbeit\\PostVersionLists\\PostId_VersionCount_SO_17-06_sample_10000_10\\files");


        LinkedList<Integer> distinctValuesOfBlockLenghtsText = new LinkedList<>();
        HashMap<Integer, Integer> frequenciesOfBlockLenghtsText = new HashMap<>();
        LinkedList<Integer> affectedPostHistoriesText = new LinkedList<>();
        LinkedList<Integer> affectedPostsText = new LinkedList<>();

        LinkedList<Integer> distinctValuesOfBlockLenghtsCode = new LinkedList<>();
        HashMap<Integer, Integer> frequenciesOfBlockLenghtsCode = new HashMap<>();
        LinkedList<Integer> affectedPostHistoriesCode = new LinkedList<>();
        LinkedList<Integer> affectedPostsCode = new LinkedList<>();

        for(String path : pathToAllDirectories) {
            PostVersionsListManagement postVersionsListManagement = new PostVersionsListManagement(path);
            for(PostVersionList postVersionList : postVersionsListManagement.postVersionLists) {
                for (PostVersion postVersion : postVersionList) {
                    for(PostBlockVersion postBlockVersion : postVersion.getPostBlocks()){

                        int blockLength = postBlockVersion.getContent().length();

                        if(blockLength <= 5){
                            if(postBlockVersion instanceof TextBlockVersion){
                                if(!distinctValuesOfBlockLenghtsText.contains(blockLength)){
                                    distinctValuesOfBlockLenghtsText.add(blockLength);
                                    frequenciesOfBlockLenghtsText.put(blockLength, 1);
                                }else{
                                    frequenciesOfBlockLenghtsText.replace(
                                            blockLength,
                                            frequenciesOfBlockLenghtsText.get(blockLength),
                                            frequenciesOfBlockLenghtsText.get(blockLength) + 1
                                    );
                                }

                                if(!affectedPostHistoriesText.contains(postVersion.getPostHistoryId()))
                                    affectedPostHistoriesText.add(postVersion.getPostHistoryId());

                                if(!affectedPostsText.contains(postVersion.getPostId()))
                                    affectedPostsText.add(postVersion.getPostId());
                            }
                            else if(postBlockVersion instanceof CodeBlockVersion){
                                if(!distinctValuesOfBlockLenghtsCode.contains(blockLength)){
                                    distinctValuesOfBlockLenghtsCode.add(blockLength);
                                    frequenciesOfBlockLenghtsCode.put(blockLength, 1);
                                }else{
                                    frequenciesOfBlockLenghtsCode.replace(
                                            blockLength,
                                            frequenciesOfBlockLenghtsCode.get(blockLength),
                                            frequenciesOfBlockLenghtsCode.get(blockLength) + 1
                                    );
                                }

                                if(!affectedPostHistoriesCode.contains(postVersion.getPostHistoryId()))
                                    affectedPostHistoriesCode.add(postVersion.getPostHistoryId());

                                if(!affectedPostsCode.contains(postVersion.getPostId()))
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

    @Test
    public void getStatisticsOfBlocksWithMultipleLinkingPossibilities(){
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader("testdata\\statistics\\possible multiple connections.csv"));

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
            while((line = bufferedReader.readLine()) != null){
                if(firstLine){
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

                if(hasPossiblePreds){
                    if(frequenciesOfBlocKTypeIds.get(blockTypeId) == null && !distinctValuesOfPossibleBlockTypeIds.contains(blockTypeId)){
                        distinctValuesOfPossibleBlockTypeIds.add(blockTypeId);
                        frequenciesOfBlocKTypeIds.put(blockTypeId, 1);
                    }else{
                        frequenciesOfBlocKTypeIds.replace(blockTypeId, frequenciesOfBlocKTypeIds.get(blockTypeId), frequenciesOfBlocKTypeIds.get(blockTypeId)+1);
                    }

                    if(frequenciesOfPossiblePredBlocks.get(numberOfPossibleLinks) == null && !distinctValuesOfPossiblePredBlocks.contains(numberOfPossibleLinks)){
                        distinctValuesOfPossiblePredBlocks.add(numberOfPossibleLinks);
                        frequenciesOfPossiblePredBlocks.put(numberOfPossibleLinks, 1);
                    }else{
                        frequenciesOfPossiblePredBlocks.replace(numberOfPossibleLinks, frequenciesOfPossiblePredBlocks.get(numberOfPossibleLinks), frequenciesOfPossiblePredBlocks.get(numberOfPossibleLinks)+1);
                    }

                    if(!distinctValuesOfPossiblePredPostHistoryIds.contains(postHistoryId)){
                        distinctValuesOfPossiblePredPostHistoryIds.add(postHistoryId);

                        if(frequenciesOfAffectedPredPostHistoryIds.get(numberOfPossibleLinks) == null){
                            frequenciesOfAffectedPredPostHistoryIds.put(numberOfPossibleLinks, 1);
                        }else{
                            frequenciesOfAffectedPredPostHistoryIds.replace(numberOfPossibleLinks, frequenciesOfAffectedPredPostHistoryIds.get(numberOfPossibleLinks), frequenciesOfAffectedPredPostHistoryIds.get(numberOfPossibleLinks)+1);
                        }
                    }
                }

                if(hasPossibleSuccs){
                    if(frequenciesOfBlocKTypeIds.get(blockTypeId) == null && !distinctValuesOfPossibleBlockTypeIds.contains(blockTypeId)){
                        distinctValuesOfPossibleBlockTypeIds.add(blockTypeId);
                        frequenciesOfBlocKTypeIds.put(blockTypeId, 1);
                    }else{
                        frequenciesOfBlocKTypeIds.replace(blockTypeId, frequenciesOfBlocKTypeIds.get(blockTypeId), frequenciesOfBlocKTypeIds.get(blockTypeId)+1);
                    }

                    if(frequenciesOfPossibleSuccBlocks.get(numberOfPossibleLinks) == null && !distinctValuesOfPossibleSuccBlocks.contains(numberOfPossibleLinks)){
                        distinctValuesOfPossibleSuccBlocks.add(numberOfPossibleLinks);
                        frequenciesOfPossibleSuccBlocks.put(numberOfPossibleLinks, 1);
                    }else{
                        frequenciesOfPossibleSuccBlocks.replace(numberOfPossibleLinks, frequenciesOfPossibleSuccBlocks.get(numberOfPossibleLinks), frequenciesOfPossibleSuccBlocks.get(numberOfPossibleLinks)+1);
                    }

                    if(!distinctValuesOfPossibleSuccPostHistoryIds.contains(postHistoryId)){
                        distinctValuesOfPossibleSuccPostHistoryIds.add(postHistoryId);

                        if(frequenciesOfAffectedSuccPostHistoryIds.get(numberOfPossibleLinks) == null){
                            frequenciesOfAffectedSuccPostHistoryIds.put(numberOfPossibleLinks, 1);
                        }else{
                            frequenciesOfAffectedSuccPostHistoryIds.replace(numberOfPossibleLinks, frequenciesOfAffectedSuccPostHistoryIds.get(numberOfPossibleLinks), frequenciesOfAffectedSuccPostHistoryIds.get(numberOfPossibleLinks)+1);
                        }
                    }
                }

                if(!postIdWithMultipleChoices.contains(postId))
                    postIdWithMultipleChoices.add(postId);
            }

            System.out.println("number of post ids that contain blocks which could be connected to multiple predecessors or successors: " +  postIdWithMultipleChoices.size() + " of 100000 (" + ((double)postIdWithMultipleChoices.size() / 100000) * 100 + " %)");
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


    @Test
    public void getAverageSizesOfBlocksAndVersions(){
        LinkedList<String> pathToAllDirectories = new LinkedList<>();

        // pathToAllDirectories.add("testdata\\representative CSVs");
        pathToAllDirectories.add("C:\\Users\\Lorik\\Desktop\\5. Semester\\Master-Arbeit\\PostVersionLists\\PostId_VersionCount_SO_17-06_sample_10000_1\\files");
        pathToAllDirectories.add("C:\\Users\\Lorik\\Desktop\\5. Semester\\Master-Arbeit\\PostVersionLists\\PostId_VersionCount_SO_17-06_sample_10000_2\\files");
        pathToAllDirectories.add("C:\\Users\\Lorik\\Desktop\\5. Semester\\Master-Arbeit\\PostVersionLists\\PostId_VersionCount_SO_17-06_sample_10000_3\\files");
        pathToAllDirectories.add("C:\\Users\\Lorik\\Desktop\\5. Semester\\Master-Arbeit\\PostVersionLists\\PostId_VersionCount_SO_17-06_sample_10000_4\\files");
        pathToAllDirectories.add("C:\\Users\\Lorik\\Desktop\\5. Semester\\Master-Arbeit\\PostVersionLists\\PostId_VersionCount_SO_17-06_sample_10000_5\\files");
        pathToAllDirectories.add("C:\\Users\\Lorik\\Desktop\\5. Semester\\Master-Arbeit\\PostVersionLists\\PostId_VersionCount_SO_17-06_sample_10000_6\\files");
        pathToAllDirectories.add("C:\\Users\\Lorik\\Desktop\\5. Semester\\Master-Arbeit\\PostVersionLists\\PostId_VersionCount_SO_17-06_sample_10000_7\\files");
        pathToAllDirectories.add("C:\\Users\\Lorik\\Desktop\\5. Semester\\Master-Arbeit\\PostVersionLists\\PostId_VersionCount_SO_17-06_sample_10000_8\\files");
        pathToAllDirectories.add("C:\\Users\\Lorik\\Desktop\\5. Semester\\Master-Arbeit\\PostVersionLists\\PostId_VersionCount_SO_17-06_sample_10000_9\\files");
        pathToAllDirectories.add("C:\\Users\\Lorik\\Desktop\\5. Semester\\Master-Arbeit\\PostVersionLists\\PostId_VersionCount_SO_17-06_sample_10000_10\\files");

        long textBlockLength = 0;
        int numberOfTextBlocks = 0;

        long codeBlockLength = 0;
        int numberOfCodeBlocks = 0;

        int numberOfVersions = 0;

        int numberOfPosts = 0;

        for(String path : pathToAllDirectories) {
            PostVersionsListManagement postVersionsListManagement = new PostVersionsListManagement(path);
            for(PostVersionList postVersionList : postVersionsListManagement.postVersionLists) {

                numberOfPosts++;

                for (PostVersion postVersion : postVersionList) {
                    for(int k=0; k<postVersion.getTextBlocks().size(); k++) {
                        textBlockLength += postVersion.getTextBlocks().get(k).getContent().length();
                    }
                    for(int k=0; k<postVersion.getCodeBlocks().size(); k++) {
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
        System.out.println("average number of versions: " + (double)numberOfVersions / numberOfPosts);
        System.out.println();
        System.out.println("number of text blocks: " + numberOfTextBlocks);
        System.out.println("average number of text blocks: " + (double)numberOfTextBlocks / numberOfVersions);
        System.out.println("number of code blocks: " + numberOfCodeBlocks);
        System.out.println("average number of code blocks: " + (double)numberOfCodeBlocks / numberOfVersions);
        System.out.println();
        System.out.println("text block length: " + textBlockLength);
        System.out.println("average text block length: " + (double)textBlockLength / numberOfTextBlocks);
        System.out.println("code block length: " + codeBlockLength);
        System.out.println("average code block length: " + (double)codeBlockLength / numberOfCodeBlocks);
    }
}
