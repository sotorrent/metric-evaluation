package de.unitrier.st.soposthistory.metricscomparison.util;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

public class CompletedCSVConverter{

    public static String convertString(File file){

        StringBuilder output = new StringBuilder();

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            boolean firstLine = true;

            String line;
            while((line = br.readLine()) != null){
                StringTokenizer tokens = new StringTokenizer(line, ";");
                int count = 0;
                while(tokens.hasMoreTokens()){
                    String token = tokens.nextToken();
                    count++;

                    if(firstLine) {
                        token = token.replaceAll("\\s+", "");
                        token = token.replaceAll("\"", "");
                        output.append(token);

                        if(count <= 6)
                            output.append(";");

                    }else if(count <= 6){
                        token = token.replaceAll("\\s+", "");
                        token = token.replaceAll("\"", "");
                        output.append(token).append(";");
                    }else{
                        output.append(token.substring(1, token.length()));
                    }
                }
                firstLine = false;
                output.append("\n");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return output.toString();
    }

    public static void convertFiles(String directoryOfCompletedFiles){

        File file = new File(directoryOfCompletedFiles);
        Pattern pattern = Pattern.compile("completed_" + "[0-9]+" + "\\.csv");
        File[] allCompletedPostVersionListsInFolder = file.listFiles((dir, name) -> name.matches(pattern.pattern())); // https://stackoverflow.com/questions/4852531/find-files-in-a-folder-using-java

        assert allCompletedPostVersionListsInFolder != null;
        for (File completedCSV : allCompletedPostVersionListsInFolder) {
            try {
                PrintWriter printWriter = new PrintWriter(new FileWriter(completedCSV + "_new", false));
                String convertedContent = CompletedCSVConverter.convertString(completedCSV);
                printWriter.write(convertedContent);
                printWriter.flush();
                printWriter.close();
            } catch (IOException e) {
                System.err.println("Failed to convert file '" + completedCSV.getName() + "'.");
                System.exit(0);
            }
        }
    }


    public static void main(String[] args){

        LinkedList<Path> samplesToConvert = new LinkedList<>();

        samplesToConvert.add(Paths.get("testdata", "Samples_100", "PostId_VersionCount_SO_17-06_sample_100_1"));
        samplesToConvert.add(Paths.get("testdata", "Samples_100", "PostId_VersionCount_SO_17-06_sample_100_1+"));
        samplesToConvert.add(Paths.get("testdata", "Samples_100", "PostId_VersionCount_SO_17-06_sample_100_2"));
        samplesToConvert.add(Paths.get("testdata", "Samples_100", "PostId_VersionCount_SO_17-06_sample_100_2+"));
        samplesToConvert.add(Paths.get("testdata", "Samples_100", "PostId_VersionCount_SO_17-06_sample_100_multiple_possible_links"));
        samplesToConvert.add(Paths.get("testdata", "Samples_100", "PostId_VersionCount_SO_Java_17-06_sample_100_1"));
        samplesToConvert.add(Paths.get("testdata", "Samples_100", "PostId_VersionCount_SO_Java_17-06_sample_100_2"));

        for(Path path : samplesToConvert){
            convertFiles(path.toString());
        }
    }
}
