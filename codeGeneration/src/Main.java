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
		Scanner input = new Scanner(System.in);
		System.out.println(
						"This is the PQ9_bus_software subsystem code generator!\n");
		System.out.println(
						"This program will automatically generate new subsystem software" +
						"based on the templates provided in ./templates\n");
		System.out.print("Please enter a name for the subsystem to generate: ");
		dirName = input.next();
		
		// Find the referenced location (relative to current)
		Path SubsFolder = Paths.get("./" + dirName);
		if (Files.exists(SubsFolder)) {
			System.out.println("Subsystem output folder " + dirName + " exists!");
			System.out.println(
							"Overwrite existing " + dirName + " folder? (Yes)/(No)");
			String response = input.next();
			while (!(response.equals("Yes") || response.equals("No"))) {
				System.out.println("Answer not recognized, use 'Yes' or 'No'");
				System.out.println(
								"Overwrite existing " + dirName + " folder? (Yes)/(No)");
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

		System.out.println(
						"Succesfully finished generating " + dirName + " Subsystem!");
		System.out.println("Please do check the output files in ./" + dirName);
		System.out.println(
						"And take any possible warnings provided above into account");
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
		ArrayList<ParamCode> params = new ArrayList<>();
		findParams(code, params);
		//ParamCode SBSYS_sensor_loop = new SBSYS_sensor_loop();
		//params.add(SBSYS_sensor_loop);
		
		//add set, get, init and mem_pool code sections for each parameter.
		int setParams = findLine("$setParams$", code);
		params.forEach(param -> code.add(setParams, param.setterFunc()));
		int getParams = findLine("$getParams$", code);
		params.forEach(param -> code.add(getParams, param.getterFunc()));
		int initParams = findLine("$initParams$", code);
		params.forEach(param -> code.add(initParams, param.initFunc()));
		int mem_pool = findLine("$mem_pool$", code);
		params.forEach(param -> code.add(mem_pool, param.memPoolStruct()));
		// miscellaneous subsystem specific code
		int sub_specific = findLine("$sub_specific$", code);
		params.forEach(param -> code.add(sub_specific, param.subSpecific()));
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

	private static void findParams(ArrayList<String> code,
	                               ArrayList<ParamCode> params) {
		// null check
		if (null == code || null == params) {
			return;
		}
		// loop through code lines, iterator for safe removal in loop
		Iterator<String> itr = code.iterator();
		while (itr.hasNext()) {
			// if next string identifies a parameter
			String str = itr.next();
			if (str.contains("$param$")) {
				// remove leading & trailing whitespace, split on spaces
				String trim = str.trim();
				String[] parts = trim.split(" ");
				String paramId = "";
				String defaultValue = "";
				// find class describing how to generate code for this param
				if (1 == parts.length) {
					System.err.println("Warning: Empty $param$ skipped from template!");
					continue;
				} else if (3 > parts.length) {
					paramId = parts[1];
					defaultValue = "default";
					System.err.println("Warning: defaults not specified! " +
									"Using code generation defaults for " + paramId + "!");
				} else {
					paramId = parts[1];
					defaultValue = parts[2];
				}
				ParamCode p = findParamCode(paramId);
				if (null != p) {
					params.add(p);
				} else {
					System.err.println("Parameter class " + parts[1] + " not found!");
				}
				// remove the tag line from code output
				itr.remove();
			}
		}
	}

	/**
	 * Finds the correct object type for a given paramId, creates an object of
	 * this type, assigns it the specified default value and returns it.
	 *
	 * @param paramId
	 *          String identifying a paramId.
	 * @return
	 *          a ParamCode object representing the specified parameter id.
	 */
	private static ParamCode findParamCode(String paramId) {
	    //TODO: wil get ugly with more params, make dictionary or other lookup
		switch (paramId) {
			case ParamDefaults.SBSYS_sensor_loop_param_id :
				return new SBSYS_sensor_loop();
			case ParamDefaults.testing_2_param_id :
				return new testing_2();
			case ParamDefaults.testing_4_param_id :
				return new testing_4();
			default :
				return null;
		}
	}

	/**
	 * Removes template comments from the code. Template comments are
	 * identified as lines starting with '//<'.
	 *
	 * @param code
	 *          Code represented as a list of Strings.
	 */
	private static void removeTemplateComments(ArrayList<String> code) {
		// null check
		if (null == code) {
			return;
		}

		// loop through code lines, iterator for safe removal in loop
		Iterator<String> itr = code.iterator();
		while (itr.hasNext()) {
			// find next and remove leading whitespace
			String str = itr.next();
			String trimStr = str.trim();
			// remove if first chars are tempalte comment identifiers
			if (trimStr.length() > 2 && trimStr.substring(0,3).equals("//<")) {
				itr.remove();
			}
		}
	}

	/**
	 * Loads the specified file into program memory per line as a list of
	 * Strings.
	 *
	 * @param fileName
	 *          The local file to read into memory.
	 * @return
	 *          An ArrayList of Strings, each element is a line from the file.
	 */
	private static ArrayList<String> readFromFile(String fileName) {
		// Make new empty list for result
		ArrayList<String> code = new ArrayList<>();
		// Find the referenced location, try to open with BufferedReader
		// try with resource -> resource gets closed automatically
		Path filePath= Paths.get(fileName);
		try (BufferedReader bufferedReader = Files.newBufferedReader(filePath,
				Charset.forName("UTF-8"))){
			// loop through each line in BufferedReader, add to result list
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
		// Find the referenced location, try to open with BufferedReader
		// try with resource -> resource gets closed automatically
		Path filePath= Paths.get(fileName);
		try (BufferedWriter br = Files.newBufferedWriter(filePath,
				Charset.forName("UTF-8"))){
			// Loop through each list item, write to the file with a linebreak
			code.forEach((str) -> {
				if (null == str) return;
				try {
					br.write(str);
					br.newLine();
				} catch (IOException e) {
					System.err.println("Error writing string" + str + "to " +
							"file!");
				}
			});
		} catch (FileNotFoundException e) {
			System.err.println("File not found!");
		} catch (IOException e) {
			System.err.println("Error writing to file!");
		}
	}
}
