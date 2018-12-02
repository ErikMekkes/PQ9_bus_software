import java.io.*;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import Utilities.Utilities;

import org.json.JSONArray;
import org.json.JSONObject;
import parameter_ids.*;

public class Main {
	// main subsystem / directory name
	private static String dirName;
	// subdirectories
	private static ArrayList<String> subDirs = new ArrayList<>();
	// files to be generated
	private static ArrayList<String> fileNames = new ArrayList<>();
	// whether or not to overwrite existing files
	private static boolean overwriteExisting = false;
	// Map of code generation classes for each parameter
	private static Map<String, Param> paramCodeGeneration;
	private static String templateDir = "templates/";
	
	private static final String SETTINGS_FILE = "settings.json";
	private static JSONObject settings;

	public static void main(String[] args) {
		String intro =
					"\nThis is the PQ9_bus_software subsystem code generator!\n" +
					"Code generator settings can be found in settings.Utilities.\n" +
					"Templates to use for generating files can be placed in the " +
					"templates folder\n";
		System.out.println(intro);
		
		settings = Utilities.readJSONFromFile(SETTINGS_FILE);
		
		ArrayList<Param> params = Utilities.readParamCSV("params.csv");
		params.forEach(par -> System.out.println(par.toString()));
		
		if (null == settings) {
			return;
		}
		
		// find main directory from settings
		dirName = settings.getString("subsystem_name");
		// find subdirectories from settings
		JSONArray subdirectories = settings.getJSONArray("subdirectories");
		for (Object folderName : subdirectories) {
			if (folderName instanceof String) {
				subDirs.add((String) folderName);
			}
		}
		
		// generate all required directories specified in settings
		System.out.println("Making required directories...");
		makeDirs();
		
		// find files to generate from settings
		JSONArray files = settings.getJSONArray("files_to_generate");
		for (Object file : files) {
			if (file instanceof JSONObject) {
				fileNames.add(((JSONObject) file).getString("filename"));
			}
		}
		
		// generate all required files specified in settings
		System.out.println("Generating specified files...");
		makeFiles();
		
		// Indicate ending for user.
		String exit =
					"\nSuccessfully finished generating " + dirName + " Subsystem!\n" +
					"Please do check the output files in ./" + dirName +
					" and take any possible warnings provided above into account";
		System.out.println(exit);
	}
	
	private static void makeDirs() {
		Path SubsFolder = Paths.get("./" + dirName);
		
		overwriteExisting = settings.getBoolean("overwrite_existing_files");
		if (Files.exists(SubsFolder) && overwriteExisting) {
			deleteDirectoryStream(SubsFolder);
		}
		
		// Try to make the main directory
		try {
			Files.createDirectory(SubsFolder);
		} catch(FileAlreadyExistsException e) {
			System.err.println("Warning: The main directory " + dirName + " already" +
							" exists!");
		} catch (IOException e) {
			System.err.println("Error: unable to create main directory " + dirName + "!");
		}
		// Try to make the subdirectories
		subDirs.forEach(dir -> {
			Path subDir = Paths.get("./" + dirName + "/" + dir);
			if (Files.exists(subDir) && overwriteExisting) {
				deleteDirectoryStream(subDir);
			}
			try {
				Files.createDirectory(subDir);
			} catch(FileAlreadyExistsException e) {
				System.err.println("Warning: The subdirectory " + dir + " already " +
								"exists!");
			} catch (IOException e) {
				System.err.println("Error: unable to create subdirectory " + dir + "!");
			}
		});
	}
	
	private static void makeFiles() {
		fileNames.forEach(fileName ->
						Utilities.writeLinesToFile(
										processTemplate(fileName),
										"./" + dirName + "/" + fileName,
										overwriteExisting));
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
		ArrayList<String> codeLines;
		// Use fm.h template to generate a fm.h file, removing template comments
		codeLines = Utilities.readLinesFromFile("templates/fm.h");
		removeTemplateComments(codeLines);
		Utilities.writeLinesToFile(codeLines,"./" + dirName + "/fm.h",
						overwriteExisting);

		// Use fm.c template to generate a fm.c file, removing template comments
		codeLines = Utilities.readLinesFromFile("templates/fm.c");
		removeTemplateComments(codeLines);
		Utilities.writeLinesToFile(codeLines,"./" + dirName + "/fm.c", overwriteExisting);
	}

	private static void generateParameterFiles() {
		ArrayList<String> codeLines;
		// generate parameters.h file, removing template comments
		codeLines = Utilities.readLinesFromFile("templates/parameters.h");
		removeTemplateComments(codeLines);
		Utilities.writeLinesToFile(codeLines,"./" + dirName + "/parameters.h", overwriteExisting);
		
		// generate parameters.c file, removing template comments
		codeLines = Utilities.readLinesFromFile("templates/parameters.c");
		removeTemplateComments(codeLines);
		processParameters(codeLines);
		Utilities.writeLinesToFile(codeLines,"./" + dirName + "/parameters.c", overwriteExisting);
	}

	private static void processParameters(ArrayList<String> code) {
		ArrayList<ParamCode> params = new ArrayList<>();
		findParams(code, params);
		
		//add set, get, init and mem_pool code sections for each parameter.
		int setParams = findLine("$setParams$", code);
		params.forEach(param -> code.add(setParams, param.setterFunc()));
		int getParams = findLine("$getParams$", code);
		params.forEach(param -> code.add(getParams, param.getterFunc()));
		int initParams = findLine("$initParams$", code);
		params.forEach(param -> code.add(initParams, param.initFunc()));
		int mem_pool = findLine("$mem_pool$", code);
		params.forEach(param -> code.add(mem_pool, param.memPoolStruct()));
		// miscellaneous parameter specific code
		int par_specific = findLine("$par_specific$", code);
		params.forEach(param -> code.add(par_specific, param.parSpecific()));
	}
	
	/**
	 * Attempts to find a line containing the specified identifier String.
	 * Returns the linenumber of first occurrence if found, return -1 otherwise.
	 *
	 * @param identifier
	 *      String to look for in specified code.
	 * @param code
	 *      List of code lines to look in.
	 * @return
	 *      Line Number, first occurrence of identifier in list of code lines.
	 */
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
				String dataType = "";
				Param defaults = null;
				int enumValue = -1;
				// find class describing how to generate code for this param
				if (1 == parts.length) {
					// $param$
					System.err.println("Warning: Empty $param$ skipped from template!");
					// skip this one
					itr.remove();
					continue;
				}
				if (1 < parts.length) {
					// load default values for parameter
					paramId = parts[1];
					defaults = paramCodeGeneration.get(paramId);
					if (null == defaults) {
						System.err.println("Parameter class " + parts[1] + " not found!" +
										"parameter not included!");
						// skip this one
						itr.remove();
						continue;
					} else {
						enumValue = defaults.enumValue;
						dataType = defaults.dataType;
						defaultValue = defaults.defaultValue;
					}
				}
				if (2 == parts.length) {
					// $param$ some_name
					System.err.println("Warning: default not specified! " +
									"Using code generation defaults for " + paramId + "!");
				} else if (3 == parts.length) {
					// $param$ some_name some_value
					if (!parts[2].equals("default")) {
						defaultValue = parts[2];
					}
				} else if (4 == parts.length) {
					// $param$ some_name some_value some_type
					if (!parts[2].equals("default")) {
						defaultValue = parts[2];
					}
					if (!parts[3].equals("default")) {
						dataType = parts[3];
					}
				} else if (5 == parts.length) {
					// $param$ some_name some_value some_type some_number
					if (!parts[2].equals("default")) {
						defaultValue = parts[2];
					}
					if (!parts[3].equals("default")) {
						dataType = parts[3];
					}
					if (!parts[4].equals("default")) {
						try {
							enumValue = Integer.parseInt(parts[4]);
						} catch (NumberFormatException e) {
							System.err.println("Error: Enum value for " + paramId + " is not " +
											"a number! parameter not included!");
						}
					}
				} else if (5 < parts.length) {
					// Even more input specified...
					// $param$ some_name some_value some_type some_number x x x x x x
					if (!parts[2].equals("default")) {
						defaultValue = parts[2];
					}
					if (!parts[3].equals("default")) {
						dataType = parts[3];
					}
					if (!parts[4].equals("default")) {
						try {
							enumValue = Integer.parseInt(parts[4]);
						} catch (NumberFormatException e) {
							System.err.println("Error: Enum value for " + paramId + " is not " +
											"a number! parameter not included!");
						}
					}
					System.err.println("Warning: Excessive input for " + paramId + " " +
									" parameters beyond enumValue ignored");
				}
				Param newParam = new Param(enumValue, paramId, dataType, defaultValue);
				ParamCode par = ParamDefaults.getCodeGeneratorClass(newParam);
				params.add(par);
				// remove the tag line from code output
				itr.remove();
			}
		}
	}
	
	/**
	 * Generates a List of code lines by processing a specified template file.
	 * A template file may define variables and additional templates.
	 *
	 * Processing first removing template comments, followed by checking for
	 * variable definitions and replacing all occurrences of variables
	 * with the values specified for each variable. Finally the template is
	 * checked for additional template definitions, which are read and
	 * processed similarly.
	 *
	 * Variables from parent templates carry over to child templates. A child
	 * template may redefine a variable with a different value, the process
	 * function will warn when this occurs to ensure intended variable usage
	 *
	 * @param templateFile
	 *      Specified template file to generate code lines from.
	 * @return
	 *      A list of code lines generated from the specified template.
	 */
	private static ArrayList<String> processTemplate(String templateFile) {
		// Use empty initial set of variables
		HashMap<String, String> vars = new HashMap<>();
		return processTemplate(templateFile, vars);
	}
	
	/**
	 * Generates a List of code lines by processing a specified template file.
	 * A template file may define variables and additional templates.
	 * Takes an initial set of defined variables as additional input.
	 *
	 * Processing first removing template comments, followed by checking for
	 * additional variable definitions and replacing all occurrences of variables
	 * with the values specified for each variable. Finally the template is
	 * checked for additional template definitions, which are read and
	 * processed similarly.
	 *
	 * Variables from parent templates carry over to child templates. A child
	 * template may redefine a variable with a different value, the process
	 * function will warn when this occurs to ensure intended variable usage
	 *
	 * @param templateFile
	 *      Specified template file to generate code lines from.
	 * @param vars
	 *      Specified initial variables as (key,value) pairs.
	 * @return
	 *      A list of code lines generated from the specified template.
	 */
	private static ArrayList<String> processTemplate(
					String templateFile,
					HashMap<String, String> vars) {
		// read code from template file
		ArrayList<String> code = Utilities.readLinesFromFile(templateDir + templateFile);
		if (null == code) {
			// nothing read from file, user is already warned
			return null;
		}
		
		removeTemplateComments(code);
		
		// check if templateFile specifies additional variables
		Iterator<String> itr = code.iterator();
		while(itr.hasNext()) {
			String str = itr.next();
			String trimStr = str.trim();
			String[] parts = trimStr.split(" ");
			if (3== parts.length && parts[0].equals("$var$")) {
				// if variable was already defined in parent, warn user
				if (vars.containsKey(parts[1])) {
					System.err.println("Warning: template " + templateFile +
									" overrides parent template variable " + parts[1] +
									" locally with value " + parts[2]);
				}
				// store variable in map as key, value
				vars.put(parts[1], parts[2]);
				itr.remove();
			}
		}
		
		// loop through code lines, replace all var keys with var values
		int lineNumber = -1;
		int size = code.size() - 1;
		while (lineNumber < size){
			lineNumber++;
			String str = code.get(lineNumber);
			// loop through vars, if str contains var key replace with var value
			for (String key : vars.keySet()) {
				String value = vars.get(key);
				str = str.replace(key, value);
			}
			code.set(lineNumber, str);
		}
		
		// check if the templateFile specifies additional templates
		lineNumber = -1;
		size = code.size() - 1;
		while (lineNumber < size){
			lineNumber++;
			String str = code.get(lineNumber);
			String trimStr = str.trim();
			String[] parts = trimStr.split(" ");
			// if current line identifies another template file...
			if (2 == parts.length && parts[0].equals("$template$")) {
				// call this function recursively for sub-template
				ArrayList<String> temp = processTemplate(parts[1], vars);
				if (null == temp) {
					// nothing read from file, user is already warned
					continue;
				}
				// replace line with code read from template
				code.remove(lineNumber);
				size--;
				code.addAll(lineNumber, temp);
				size += temp.size();
			}
		}
		
		return code;
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
}