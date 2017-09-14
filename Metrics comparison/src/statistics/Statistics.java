package statistics;

import csvExtraction.PostVersionsListManagement;
import de.unitrier.st.soposthistory.blocks.PostBlockVersion;
import de.unitrier.st.soposthistory.version.PostVersionList;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.StringTokenizer;

public class Statistics {

    // TODO: find more ideas for statistics

    @Test
    public void testPostVersionListManagement(){
        LinkedList<String> pathToAllDirectories = new LinkedList<>();

        pathToAllDirectories.add("postVersionLists");
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

        output.append("post-id; post-history-id; local-id; possible pred or succ local-ids; number of possible successors or predecessors\n");

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
    public void getStatistics(){
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader("testdata\\statistics\\possible multiple connections.csv"));

            LinkedList<Integer> postIdWithMultipleChoices = new LinkedList<>();

            LinkedList<Integer> distinctValuesOfPossiblePreds = new LinkedList<>();
            HashMap<Integer, Integer> frequenciesOfPossiblePreds = new HashMap<>();

            LinkedList<Integer> distinctValuesOfPossibleSuccs = new LinkedList<>();
            HashMap<Integer, Integer> frequenciesOfPossibleSuccs = new HashMap<>();

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

                String predOrSuccs = tokens.nextToken();
                Boolean hasPossiblePreds = predOrSuccs.contains("pred");
                Boolean hasPossibleSuccs = predOrSuccs.contains("succ");
                String localIds = predOrSuccs.substring(28);
                Integer numberOfPossibleLinks = Integer.valueOf(tokens.nextToken().trim());

                if(hasPossiblePreds){
                    if(frequenciesOfPossiblePreds.get(numberOfPossibleLinks) == null && !distinctValuesOfPossiblePreds.contains(numberOfPossibleLinks)){
                        distinctValuesOfPossiblePreds.add(numberOfPossibleLinks);
                        frequenciesOfPossiblePreds.put(numberOfPossibleLinks, 1);
                    }else{
                        frequenciesOfPossiblePreds.replace(numberOfPossibleLinks, frequenciesOfPossiblePreds.get(numberOfPossibleLinks), frequenciesOfPossiblePreds.get(numberOfPossibleLinks)+1);
                    }
                }
                if(hasPossibleSuccs){
                    if(frequenciesOfPossibleSuccs.get(numberOfPossibleLinks) == null && !distinctValuesOfPossibleSuccs.contains(numberOfPossibleLinks)){
                        distinctValuesOfPossibleSuccs.add(numberOfPossibleLinks);
                        frequenciesOfPossibleSuccs.put(numberOfPossibleLinks, 1);
                    }else{
                        frequenciesOfPossibleSuccs.replace(numberOfPossibleLinks, frequenciesOfPossibleSuccs.get(numberOfPossibleLinks), frequenciesOfPossibleSuccs.get(numberOfPossibleLinks)+1);
                    }
                }

                if(!postIdWithMultipleChoices.contains(postId))
                    postIdWithMultipleChoices.add(postId);
            }

            System.out.println("number of post ids that contain blocks which could be connected to multiple predecessors or successors: " +  postIdWithMultipleChoices.size() + " of 100000 (" + ((double)postIdWithMultipleChoices.size() / 100000) * 100 + " %)");
            System.out.println();

            System.out.println("Occuring numbers of blocks that could be linked and their frequencies (predecessors): ");
            Collections.sort(distinctValuesOfPossiblePreds);
            for (Integer value : distinctValuesOfPossiblePreds) {
                System.out.println(value + ": " + frequenciesOfPossiblePreds.get(value));
            }
            System.out.println();

            System.out.println("Occuring numbers of blocks that could be linked and their frequencies (successors): ");
            Collections.sort(distinctValuesOfPossibleSuccs);
            for (Integer value : distinctValuesOfPossibleSuccs) {
                System.out.println(value + ": " + frequenciesOfPossibleSuccs.get(value));
            }


        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
