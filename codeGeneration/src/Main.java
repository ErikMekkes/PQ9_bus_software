import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Scanner;

import parameter_ids.*;

public class Main {
    private static ArrayList<String> codeLines;

    // Create folder structure
    private static String dirName = "TEST";
    private static String subDir1 = "HAL";

    public static void main(String[] args) {
        // Find the referenced location (relative to current)
        Path SubsFolder = Paths.get("./" + dirName);
        if (Files.exists(SubsFolder)) {
            System.out.println("Subsystem output folder " + dirName + " exists!");
            System.out.println("Overwrite existing " + dirName + " folder? (Yes)/(No)");
            Scanner input = new Scanner(System.in);
            String response = input.next();
            while (!(response.equals("Yes") || response.equals("No"))) {
                System.out.println("Answer not recognized, use 'Yes' or 'No'");
                System.out.println("Overwrite existing " + dirName + " folder? (Yes)/(No)");
                response = input.next();
            }
            switch (response){
                case "No" :
                    return;
                case "Yes" :
                    deleteDirectoryStream(SubsFolder);
            }

        }
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

        generateParameterFiles();
        generateFmFiles();

        System.out.println("Finished generating " + dirName + "Subsystem!");
    }

    private static void deleteDirectoryStream(Path path) {
        try {
            Files.walk(path)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            System.out.println("Error deleting " + dirName + " folder!");
        }
    }

    private static void generateFmFiles() {
        // Use fm.h template to generate a fm.h file, removing template comments
        codeLines = readFromFile("templates/fm.h");
        removeTemplateComments(codeLines);
        writeToFile(codeLines,"./" + dirName + "/fm.h");

        // Use fm.c template to generate a fm.c file, removing template comments
        codeLines = readFromFile("templates/fm.c");
        removeTemplateComments(codeLines);
        writeToFile(codeLines,"./" + dirName + "/fm.c");
    }

    private static void generateParameterFiles() {
        // Use fm.c template to generate a fm.c file, removing template comments
        codeLines = readFromFile("templates/parameters.h");
        removeTemplateComments(codeLines);
        writeToFile(codeLines,"./" + dirName + "/parameters.h");

        // Use fm.c template to generate a fm.c file, removing template comments
        codeLines = readFromFile("templates/parameters.c");
        removeTemplateComments(codeLines);
        processParameters(codeLines);
        writeToFile(codeLines,"./" + dirName + "/parameters.c");
    }

    private static void processParameters(ArrayList<String> code) {
        ArrayList<ParamID> params = new ArrayList<>();
        ParamID SBSYS_sensor_loop = new SBSYS_sensor_loop_param_id_32();
        params.add(SBSYS_sensor_loop);

        //add set, get, init and struct code sections for each parameter.
        params.forEach(param -> code.add(findLine("$setParams$",code),param.setterFunc()));
        params.forEach(param -> code.add(findLine("$getParams$",code),param.getterFunc()));
        params.forEach(param -> code.add(findLine("$initParams$",code),param.initFunc()));
        params.forEach(param -> code.add(findLine("$mem_pool$",code),param.memPoolStruct()));
    }

    private static int findLine(String identifier, ArrayList<String> code) {
        Iterator<String> itr = code.iterator();
        int res = -1;
        while (itr.hasNext()) {
            res++;
            String str = itr.next();
            if (str.contains(identifier)) {
                itr.remove();
                break;
            }
        }
        return res;
    }

    /**
     * Removes template comments from the code. Template comments are identified as lines starting with '//<'.
     * @param code
     *          Code represented as a list of Strings.
     */
    private static void removeTemplateComments(ArrayList<String> code) {
        if (null == code) {
            return;
        }

        Iterator<String> itr = code.iterator();
        while (itr.hasNext()) {
            String str = itr.next();
            String trimStr = str.trim();
            if (trimStr.length() > 2 && trimStr.substring(0,3).equals("//<")) {
                itr.remove();
            }
        }
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
