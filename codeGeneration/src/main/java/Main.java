import java.io.*;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import org.json.JSONArray;
import org.json.JSONObject;
import parameter_ids.*;

public class Main {
	// main subsystem / directory name
	private static String dirName;
	// subdirectories
	private static ArrayList<String> subDirs = new ArrayList<>();
	// files to be generated and their base templates
	private static Map<String, String> files = new HashMap<>();
	// whether or not to overwrite existing files
	private static boolean overwriteExisting = false;
	// Map of all available parameters
	private static Map<String, Param> params = new HashMap<>();
	// Map of parameters to use for each file
	private static Map<String, Map<String, Param>> fileParams =
					new HashMap<>();
	
	
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
		
		// Create map of all available parameters
		ArrayList<Param> pars = Utilities.readParamCSV("params.csv");
		pars.forEach(par -> params.put(par.name, par));
		
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
		// files in settings.json should be an object array = [{..},{..},...]
		JSONArray sFiles = settings.getJSONArray("files_to_generate");
		for (Object file : sFiles) {
			if (file instanceof JSONObject) {
				// if a base template was specified, use that template
				if (((JSONObject) file).has("base_template")) {
					files.put(((JSONObject) file).getString("filename"),
									((JSONObject) file).getString("base_template"));
				} else {
					// use the filename to search for a base template
					files.put(((JSONObject) file).getString("filename"),
									((JSONObject) file).getString("filename"));
				}
			}
		}
		
		findParams();
		
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
	
	private static void findParams() {
		// find parameters to use for each file
		JSONArray sFiles = settings.getJSONArray("files_to_generate");
		for (Object file : sFiles) {
			if (file instanceof JSONObject) {
				// each file object should have a specified filename key : value pair
				String fileName = ((JSONObject) file).getString("filename");
				// make empty list of params belonging to this file
				Map<String, Param> filePars = new HashMap<>();
				// each file object might have a parameter array
				if (((JSONObject) file).has("parameters")) {
					// make and add a param object for each element in the array
					Object parameters = ((JSONObject) file).get(
									"parameters");
					// parameters is a filename containing parameter descriptions
					if (parameters instanceof String) {
						ArrayList<String> lines =
										Utilities.readLinesFromFile((String) parameters);
						if (null == lines) {
							System.err.println("Eror: Parameter file " + parameters + " for" +
											" " + fileName + " not found!");
							continue;
						}
						lines.forEach(pString -> {
							String[] parts = pString.split(",");
							Param p = new Param(parts[0], parts[1], parts[2], parts[3]);
							filePars.put(p.name, p);
						});
					}
					// parameters is an array containing parameter dsecriptions
					if (parameters instanceof JSONArray) {
						((JSONArray) parameters).forEach(par -> {
							// description is a default parameter name
							if (par instanceof String) {
								Param p = params.get(par);
								filePars.put(p.name, p);
							}
							// description is a new custom parameter
							if (par instanceof JSONArray) {
								Param p = new Param((JSONArray) par);
								filePars.put(p.name, p);
							}
						});
					}
				}
				// add the list of parameters for this file to the map
				fileParams.put(fileName, filePars);
			}
		}
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
		files.forEach((fileName,baseTemplate) -> {
			Map<String, Param> pars = fileParams.get(fileName);
			ArrayList<String> codeLines = processTemplate(baseTemplate, pars);
			processParams(codeLines, fileName);
			Utilities.writeLinesToFile(
							codeLines,
							"./" + dirName + "/" + fileName,
							overwriteExisting
			);
		});
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
	
	private static void processParams(ArrayList<String> lines, String fileName) {
		Iterator<String> itr = lines.iterator();
		int lineNumber = -1;
		int size = lines.size() - 1;
		while (lineNumber < size){
			lineNumber++;
			
			String line = lines.get(lineNumber);
			String trimLine = line.trim();
			String[] parts = trimLine.split(" ");
			if (2 == parts.length && parts[0].equals("$param$")) {
				StringBuilder code = new StringBuilder();
				String codeSection = parts[1];
				// generate code for each param here
				Map<String, Param> pars = fileParams.get(fileName);
				pars.forEach((name, par) -> {
					code.append("\n");
					code.append("Add ");
					code.append(par.name);
					code.append(" parameter ");
					code.append(codeSection);
					code.append(" code section here!");
					code.append("\n");
				});
				lines.remove(lineNumber);
				lines.add(lineNumber, code.toString());
			}
		}
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
				int id = -1;
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
						id = defaults.id;
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
							id = Integer.parseInt(parts[4]);
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
							id = Integer.parseInt(parts[4]);
						} catch (NumberFormatException e) {
							System.err.println("Error: Enum value for " + paramId + " is not " +
											"a number! parameter not included!");
						}
					}
					System.err.println("Warning: Excessive input for " + paramId + " " +
									" parameters beyond enumValue ignored");
				}
				Param newParam = new Param(id, paramId, dataType, defaultValue);
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
	private static ArrayList<String> processTemplate(
					String templateFile,
					Map<String, Param> parameters) {
		// Use empty initial set of variables
		HashMap<String, String> vars = new HashMap<>();
		return processTemplate(templateFile, parameters, vars);
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
	 * @param variables
	 *      Specified initial variables as (key,value) pairs.
	 * @return
	 *      A list of code lines generated from the specified template.
	 */
	private static ArrayList<String> processTemplate(
					String templateFile,
					Map<String, Param> parameters,
					HashMap<String, String> variables) {
		// make local copy so parent variables aren't modified
		HashMap<String, String> vars = new HashMap<>(variables);
		// read code from template file
		ArrayList<String> code = Utilities.readLinesFromFile(templateDir + templateFile);
		if (null == code) {
			// nothing read from file, user is already warned
			return null;
		}
		
		// remove comments beforehand just in case to prevent misinterpretation
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
		
		// check for commands
		lineNumber = -1;
		size = code.size() - 1;
		while (lineNumber < size){
			lineNumber++;
			String line = code.get(lineNumber);
			ArrayList<String> temp = parseCommand(line, parameters, variables);
			if (null != temp) {
				// line was a command and should be replaced with generated code
				code.remove(lineNumber);
				size--;
				code.addAll(lineNumber, temp);
				size += temp.size();
				// recheck the current linenumber if line removed and nothing added
				if (temp.size() == 0) {
					lineNumber--;
				}
			}
		}
		return code;
	}
	
	// process a template in order to fill in specific parameter values
	private static ArrayList<String> processTemplate(
					String templateFile,
					Map<String, Param> parameters,
					HashMap<String, String> variables,
					Param param
	) {
		ArrayList<String> lines = processTemplate(templateFile, parameters,
						variables);
		fillInParam(lines, param);
		return lines;
	}
	
	// command results in a new set of code lines
	public static ArrayList<String> parseCommand(
					String line,
					Map<String, Param> parameters,
					HashMap<String, String> variables
	) {
		// try to find $command$ section in provided line
		if (null == line) {
			System.err.println("Error: Line to check for command was null!");
			return null;
		}
		int c_start = line.indexOf('$');
		if (-1 == c_start) {
			// no starting $ found, not a command -> no additional action needed
			return null;
		} else {
			// need to parse what's in between $ characters...
			c_start = c_start +1;
		}
		int c_end = line.indexOf('$', c_start);
		if (-1 == c_end) {
			System.err.println("Error: No end '$' found for command in line: " +
							line + " : Line removed!");
			return new ArrayList<>();
		}
		
		// know what the command inbetween command identifier $$ is -> execute it
		String cmd = line.substring(c_start, c_end);
		switch (cmd) {
			case "p-template" :
				return pTemplateCmd(line, c_end, parameters, variables);
			case "p-line" :
				return pLineCmd(line, c_end, parameters);
			case "template" :
				return templateCmd(line, c_end, parameters, variables);
			default :
				System.err.println("Error: Unrecognized command : " + cmd);
				return new ArrayList<>();
		}
	}
	
	private static ArrayList<String> pTemplateCmd(
					String line,
					int index,
					Map<String, Param> parameters,
					HashMap<String, String> variables
	) {
		if (null == parameters) {
			return null;
		}
		ArrayList<String> lines = new ArrayList<>();
		int p_list_start = line.indexOf('[', index);
		if (-1 == p_list_start) {
			System.err.println("Error: no parameter list provided for p-line " +
							"command : " + line + " : Line removed!");
			return new ArrayList<>();
		} else {
			// need to read whats in between list identifiers
			p_list_start = p_list_start + 1;
		}
		int p_list_end = line.indexOf(']', index);
		if (-1 == p_list_end) {
			System.err.println("Error: badly formatted parameter list provided " +
							"for p-line command : " + line + " : Line removed!");
			return new ArrayList<>();
		}
		// have a template to fill in for a list of parameters
		String[] params = line.substring(p_list_start, p_list_end).split("\\|");
		String template = line.substring(p_list_end + 2);
		// if keyword all is used in list -> fill template for all params
		for (String p : params) {
			if (p.equals("all")) {
				// for all parameters
				parameters.forEach((name, par) -> {
					// fill in the template with values of par
					ArrayList<String> newLines = processTemplate(template, null,
									variables, par);
					if (null != newLines) {
						lines.addAll(newLines);
					}
				});
				return lines;
			}
		}
		// only fill template for specified parameters
		for (String p : params) {
			// fill in the template
			Param par = parameters.get(p);
			if (null == par) {
				System.err.println("Error: Unknown parameter : " + p + " : Parameter " +
								"skipped!");
				continue;
			}
			ArrayList<String> newLines = processTemplate(template, null,
							variables, par);
			if (null != newLines) {
				lines.addAll(newLines);
			}
		}
		return lines;
	}
	
	private static ArrayList<String> pLineCmd(
					String line,
					int index,
					Map<String, Param> parameters
	) {
		ArrayList<String> lines = new ArrayList<>();
		int p_list_start = line.indexOf('[', index);
		if (-1 == p_list_start) {
			System.err.println("Error: no parameter list provided for p-line " +
							"command : " + line + " : Line removed!");
			return new ArrayList<>();
		} else {
			// need to read whats in between list identifiers
			p_list_start = p_list_start + 1;
		}
		int p_list_end = line.indexOf(']', p_list_start);
		if (-1 == p_list_end) {
			System.err.println("Error: badly formatted parameter list provided " +
							"for p-line command : " + line + " : Line removed!");
			return new ArrayList<>();
		}
		// have a code line to fill in for a list of parameters
		String[] params = line.substring(p_list_start, p_list_end).split("\\|");
		String paramLine = line.substring(p_list_end + 2);
		// if keyword all is used in list -> fill line for all params
		for (String p : params) {
			if (p.equals("all")) {
				// for all parameters
				parameters.forEach((name, par) -> {
					// fill in the code line with values of par
					lines.add(fillInParam(paramLine, par));
				});
				return lines;
			}
		}
		// only fill line for specified parameters
		for (String p : params) {
			// fill in the template
			Param par = parameters.get(p);
			if (null == par) {
				System.err.println("Error: Unknown parameter : " + p + " : Parameter " +
								"skipped!");
				continue;
			}
			lines.add(fillInParam(paramLine, par));
		}
		return lines;
	}
	
	private static ArrayList<String> templateCmd(
					String line,
					int index,
					Map<String, Param> parameters,
					HashMap<String, String> variables
	) {
		int t_name_start = index + 2;
		if (t_name_start >= line.length()) {
			System.err.println("Error: No template name specified for template " +
							"command : " + line + " : Line removed!");
			return new ArrayList<>();
		}
		String templateName = line.substring(t_name_start);
		ArrayList<String> newLines = processTemplate(templateName, parameters, variables);
		if (null == newLines) {
			// template not found, user is already warned, return empty
			return new ArrayList<>();
		}
		return newLines;
	}
	
	private static void fillInParam(ArrayList<String> lines, Param param) {
		if (null == lines) {
			return;
		}
		// loop through code lines, replace all var keys with var values
		int lineNumber = -1;
		int size = lines.size() - 1;
		while (lineNumber < size){
			lineNumber++;
			String line = lines.get(lineNumber);
			line = fillInParam(line, param);
			lines.set(lineNumber, line);
		}
	}
	
	private static String fillInParam(String line, Param param) {
			line = line.replace("id", param.id + "");
			line = line.replace("p_dataType", param.dataType);
			line = line.replace("p_enumName", param.enumName);
			line = line.replace("p_name", param.name);
			line = line.replace("p_defaultValue", param.defaultValue);
			return line;
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
