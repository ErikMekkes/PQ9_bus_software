import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Main {
    private static ArrayList<String> codeLines;

    public static void main(String[] args) {
        // Create folder structure
        String dirName = "TEST";
        String subDir1 = "HAL";
        // Find the referenced location (relative to current)
        Path SubsFolder = Paths.get("./" + dirName);
        Path HALFolder = Paths.get("./" + dirName + "/" + subDir1);
        // Try to make the directories
        try {
            Files.createDirectory(SubsFolder);
            Files.createDirectory(HALFolder);
        } catch(FileAlreadyExistsException e) {
            System.err.println("One of the files to create already exists!");
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Use fm.h template to generate a fm.h file, removing template comments
        codeLines = readFromFile("templates/fm.h");
        removeTemplateComments(codeLines);
        writeToFile(codeLines,"./" + dirName + "/fm.h");

        // Use fm.c template to generate a fm.c file, removing template comments
        codeLines = readFromFile("templates/fm.c");
        removeTemplateComments(codeLines);
        writeToFile(codeLines,"./" + dirName + "/fm.c");
    }

    /**
     * Removes template comments from the code. Template comments are identified as lines starting with '//<'.
     * @param code
     *          Code represented as a list of Strings.
     */
    private static void removeTemplateComments(ArrayList<String> code) {
        code.removeIf(str -> str.length() > 2 && str.substring(0,3).equals("//<"));
    }

    /**
     * Loads the specified file into program memory per line as a list of Strings.
     *
     * @param fileName
     *          The local file to read into memory.
     * @return
     *          An ArrayList of Strings, each element is a line from the file.
     */
    private static ArrayList<String> readFromFile(String fileName) {
        // Make new empty list
        ArrayList<String> code = new ArrayList<>();
        // Find the referenced location.
        Path filePath= Paths.get(fileName);
        // Try to open the file as a BufferedReader resource (closed automatically)
        try (BufferedReader bufferedReader = Files.newBufferedReader(filePath, Charset.forName("UTF-8"))){
            // Access the BufferedReader loop through the line, adding them to the list
            bufferedReader.lines().forEach(code::add);
            return code;
        } catch (FileNotFoundException e) {
            System.err.println("File not found!");
            return null;
        } catch (IOException e) {
            System.err.println("Error reading from file!");
            return null;
        }
    }

    /**
     * Writes the specified list of Strings to a file as separate lines.
     * @param code
     *          ArrayList of Strings to write to file.
     * @param fileName
     *
     */
    private static void writeToFile(ArrayList<String> code, String fileName) {
        // Find the referenced location.
        Path filePath= Paths.get(fileName);
        // Try to open the file as a BufferedWriter resource (closed automatically)
        try (BufferedWriter br = Files.newBufferedWriter(filePath, Charset.forName("UTF-8"))){
            // Loop through each list item, write it to the file with a linebreak
            code.forEach((str) -> {
                try {
                    br.write(str);
                    br.newLine();
                } catch (IOException e) {
                    System.err.println("Error writing string" + str +"to file!");
                }
            });
        } catch (FileNotFoundException e) {
            System.err.println("File not found!");
        } catch (IOException e) {
            System.err.println("Error writing to file!");
        }
    }
}
