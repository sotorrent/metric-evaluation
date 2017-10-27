package de.unitrier.st.soposthistory.metricscomparison.tests;

import de.unitrier.st.soposthistory.metricscomparison.util.CompletedCSVConverter;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class CompletedCSVConverterTest {

    @Test
    void testStringConverting() {
        Path pathToTargetFile = Paths.get("testdata", "gt", "completed_3758880.csv");
        Path pathToSourceFile = Paths.get("testdata", "fileConverting", "wrongCSVformat", "completed_3758880.csv");

        StringBuilder targetString = new StringBuilder();
        StringBuilder sourceString = new StringBuilder();

        // get test
        try {
            BufferedReader br = new BufferedReader(new FileReader(pathToSourceFile.toString()));
            String line;
            while ((line = br.readLine()) != null) {
                sourceString.append(line).append("\n");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        // get target
        try {
            BufferedReader br = new BufferedReader(new FileReader(pathToTargetFile.toString()));
            String line;
            while ((line = br.readLine()) != null) {
                targetString.append(line).append("\n");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        assertNotEquals(sourceString, targetString);
        assertThat(CompletedCSVConverter.convertString(new File(String.valueOf(pathToSourceFile))), is(targetString.toString()));
    }

    @Test
    void testFileConvertingInDirectory() {
        Path pathToTargetFile = Paths.get("testdata", "gt", "completed_3758880.csv");
        Path pathToSourceFile = Paths.get("testdata", "fileConverting", "wrongCSVformat", "completed_3758880.csv");
        Path pathToTestFileSource = Paths.get("testdata", "fileConverting", "completed_3758880.csv");
        Path pathToTestFileTarget = Paths.get("testdata", "fileConverting", "completed_3758880.csv_new");

        StringBuilder targetString = new StringBuilder();
        StringBuilder sourceString = new StringBuilder();
        StringBuilder testStringSource = new StringBuilder();
        StringBuilder testStringTarget = new StringBuilder();

        // source to string
        getContent(sourceString, pathToSourceFile.toString());

        // target to string
        getContent(targetString, pathToTargetFile.toString());

        // copy source to testing directory
        try {
            BufferedReader br = new BufferedReader(new FileReader(String.valueOf(pathToSourceFile)));
            FileWriter fileWriter = new FileWriter(String.valueOf(pathToTestFileSource), false);
            String line;
            while ((line = br.readLine()) != null) {
                fileWriter.write(line + "\n");
            }
            br.close();
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // test to string source
        getContent(testStringSource, pathToTestFileSource.toString());

        CompletedCSVConverter.convertFiles(Paths.get("testdata", "fileConverting").toString());

        // test to string target
        getContent(testStringTarget, pathToTestFileTarget.toString());


        assertNotEquals(sourceString.toString(), testStringTarget.toString());
        assertNotEquals(targetString.toString(), testStringSource.toString());

        assertEquals(sourceString.toString(), testStringSource.toString());
        assertEquals(targetString.toString(), testStringTarget.toString());
    }

    private void getContent(StringBuilder stringBuilder, String path) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(path));
            String line;
            while ((line = br.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
