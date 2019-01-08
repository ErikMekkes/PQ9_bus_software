import java.io.*;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Main program, starting point for executable jar file.
 */
public class Main {
	// Program defaults
	private static final String TEMPLATE_EXTENSION = ".cgen_template";
	private static final int EXTENSION_LENGTH = 14;
	private static final String TEMPLATE_DIR = "templates/";
	private static final String SETTINGS_FILE = "settings.json";
	// main parameter file location
	private static String parameters_file = "params.csv";
	// whether or not to overwrite existing files
	private static boolean overwrite_existing = true;
	// whether or not to keep indentation for sub-templates
	private static boolean continue_indentation = true;
	// start point for autoincrement parameter ids;
	private static int auto_increment_start_id = -1;
	
	// settings json file
	private static JSONObject settings;
	// main subsystem / directory name
	private static String subSysName;
	// subdirectories
	private static ArrayList<String> subDirs = new ArrayList<>();
	// files to be generated and their base templates
	private static Map<String, String> files = new HashMap<>();
	// Map of all available parameters
	private static Map<String, Param> params = new HashMap<>();
	private static ArrayList<Param> params_list = new ArrayList<>();
	// Map of parameters to use for each file
	private static Map<String, Map<String, Param>> fileParams =
					new HashMap<>();
	
	/**
	 * Main program, starting point for executable jar file.
	 *
	 * @param args
	 *      Arguments provided to program when starting the program.
	 */
	public static void main(String[] args) {
		String intro =
					"\nThis is the PQ9_bus_software subsystem code generator!\n" +
					"Code generator settings can be found in settings.json.\n" +
					"Templates to use for generating files can be placed in the " +
					"templates folder\n";
		System.out.println(intro);
		
		settings = Utilities.readJSONFromFile(SETTINGS_FILE);
		
		if (null == settings) {
			return;
		}
		
		loadSettings();
		
		// Create map of all available parameters
		params_list = Utilities.readParamCSV("params.csv", auto_increment_start_id);
		params_list.forEach(par -> params.put(par.name, par));
		
		
		// find main directory from settings
		subSysName = settings.getString("subsystem_name");
		// find subdirectories from settings
		JSONArray subdirectories = settings.getJSONArray("subdirectories");
		for (Object folderName : subdirectories) {
			if (folderName instanceof String) {
				subDirs.add((String) folderName);
			}
		}
		
		// generate all required directories specified in settings
		System.out.println("\nMaking required directories...");
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
									((JSONObject) file).getString("filename") +
													TEMPLATE_EXTENSION);
				}
			}
		}
		
		loadParams();
		
		// generate all required files specified in settings
		System.out.println("Generating specified files...");
		makeFiles();
		
		// Indicate ending for user.
		String exit =
					"\nSuccessfully finished generating " + subSysName + " Subsystem!\n" +
					"Please do check the output files in ./" + subSysName +
					" and take any possible warnings provided above into account";
		System.out.println(exit);
	}
	
	/**
	 * Loads various settings from the loaded json settings file.
	 */
	private static void loadSettings() {
		// overwriting existing files
		if (settings.has("overwrite_existing_files")) {
			if (settings.get("overwrite_existing_files") instanceof Boolean) {
				overwrite_existing = settings.getBoolean("overwrite_existing_files");
			}
		}
		
		// keep indentation for subtemplates
		if (settings.has("continue_indentation")) {
			if (settings.get("continue_indentation") instanceof Boolean) {
				continue_indentation = settings.getBoolean("continue_indentation");
			}
		}
		
		// alternative default parameters csv file
		if (settings.has("parameters")) {
			if (settings.get("parameters") instanceof String) {
				parameters_file = (String) settings.get("parameters");
			}
		}
		
		// enable and set auto increment start id for parameter ids
		if (settings.has("auto_increment_start_id")) {
			if (settings.get("auto_increment_start_id") instanceof Integer) {
				auto_increment_start_id = settings.getInt("auto_increment_start_id");
			}
		}
	}
	
	/**
	 * Loads specified parameters per file from settings.json.
	 */
	private static void loadParams() {
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
						ArrayList<Param> pars =
										Utilities.readParamCSV((String) parameters, -1);
						pars.forEach(p -> filePars.put(p.name, p));
					} else if (parameters instanceof JSONArray) {
						// parameters is an array containing parameter dsecriptions
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
				} else {
					params_list.forEach(p -> filePars.put(p.name, p));
				}
				// add the list of parameters for this file to the map
				fileParams.put(fileName, filePars);
			}
		}
	}
	
	/**
	 * Checks if any of the specified directories should be created.
	 *
	 * Deletes pre-existing directories if overwrite existing is enabled.
	 * TODO: this is probably really bad, also removes other files added there
	 */
	private static void makeDirs() {
		Path SubsFolder = Paths.get("./" + subSysName);
		
		if (Files.exists(SubsFolder) && overwrite_existing) {
			deleteDirectoryStream(SubsFolder);
		}
		
		// Try to make the main directory
		try {
			Files.createDirectory(SubsFolder);
		} catch(FileAlreadyExistsException e) {
			System.err.println("Warning: The main directory " + subSysName + " already" +
							" exists!");
		} catch (IOException e) {
			System.err.println("Error: unable to create main directory " + subSysName + "!");
		}
		// Try to make the subdirectories
		subDirs.forEach(dir -> {
			Path subDir = Paths.get("./" + subSysName + "/" + dir);
			if (Files.exists(subDir) && overwrite_existing) {
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
	
	/**
	 * Checks if any of the specified files should be created or overwritten.
	 *
	 * Set of files to create is specified by settings.json
	 * Settings.json also specifies whether files should be overwritten.
	 */
	private static void makeFiles() {
		files.forEach((fileName,baseTemplate) -> {
			System.out.println("\nProcessing file : " + fileName);
			Map<String, Param> pars = fileParams.get(fileName);
			ArrayList<String> codeLines = processTemplate(baseTemplate, pars);
			Utilities.writeLinesToFile(
							codeLines,
							"./" + subSysName + "/" + fileName, overwrite_existing
			);
		});
	}
	
	/**
	 * Deletes directories and subdirectories of the specified path.
	 *
	 * Used when overwrite existing files is enabled.
	 *
	 * @param path
	 *      Starting path to delete
	 */
	private static void deleteDirectoryStream(Path path) {
		try {
			Files.walk(path)
					.sorted(Comparator.reverseOrder())
					.map(Path::toFile)
					.forEach(File::delete);
		} catch (IOException e) {
			System.out.println("Error deleting " + subSysName + " folder!");
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
		vars.put("s#name", subSysName);
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
					Map<String, String> variables
	) {
		// read code from template file
		ArrayList<String> code = Utilities.readLinesFromFile(TEMPLATE_DIR + templateFile);
		
		return processTemplate(templateFile, code, parameters, variables);
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
	 * @param code
	 *      Specified template lines to generate code lines from.
	 * @param variables
	 *      Specified initial variables as (key,value) pairs.
	 * @return
	 *      A list of code lines generated from the specified template.
	 */
	private static ArrayList<String> processTemplate(
					String templateFile,
					ArrayList<String> code,
					Map<String, Param> parameters,
					Map<String, String> variables
	) {
		if (null == code) {
			// no template lines, user is already warned
			return null;
		}
		
		// make local copy so parent variables aren't modified
		HashMap<String, String> vars = new HashMap<>(variables);
		
		// remove comments beforehand just in case to prevent misinterpretation
		removeTemplateComments(code);
		
		// check if templateFile specifies additional variables
		parseVariables(templateFile, code, vars);
		
		// loop through code lines, replace all var keys with var values
		fillInVariables(code, vars);
		
		// check for commands
		parseCommands(code, parameters, vars);
		
		return code;
	}
	
	/**
	 * Loops through the template to check for additional variable definitions.
	 *
	 * @param template
	 *        Filename of template that is being checked.
	 * @param code
	 *        Contents of the template file.
	 * @param variables
	 *        Existing set of variables to add to.
	 */
	private static void parseVariables(
					String template,
					ArrayList<String> code,
					HashMap<String, String> variables
	) {
		// loop through lines of code
		int lineNumber = -1;
		int size = code.size() - 1;
		while (lineNumber < size) {
			lineNumber++;
			// check if line is a variable definition
			String line = code.get(lineNumber);
			if (parseVariable(line, template, variables)) {
				// remove line from output
				code.remove(lineNumber);
				size--;
				lineNumber--;
			}
		}
	}
	
	/**
	 * Checks if a template line defines an additional variable.
	 *
	 * @param line
	 *      Line to check.
	 * @param template
	 *      Filename of template that is being checked.
	 * @param variables
	 *      Existing set of variables to add to.
	 * @return
	 *      Whether a variable definition was found.
	 */
	private static boolean parseVariable(
					String line,
					String template,
					Map<String, String> variables
	) {
		// try to find $command$ section in provided line
		if (null == line) {
			System.err.println("Error: Line to check for command was null!");
			return false;
		}
		int c_start = line.indexOf('$');
		if (-1 == c_start) {
			// no starting $ found, not a command -> no additional action needed
			return false;
		} else {
			// need to parse what's in between $ characters...
			c_start = c_start +1;
		}
		int c_end = line.indexOf('$', c_start);
		if (-1 == c_end) {
			System.err.println("Error: No end '$' found for command in line: " +
							line + " : Line ignored!");
			return true;
		}
		
		// found command in between $$ command identifiers -> execute command
		String cmd = line.substring(c_start, c_end);
		if (cmd.equals("var")) {
			int v_name_start = line.indexOf("\\{");
			if (-1 == v_name_start) {
				// no starting '#', no name specified
				System.err.println("Error: no starting '#' found for variable name " +
								"in line :" + line + " : Line ignored!");
				return true;
			} else {
				// need to parse what's in between '#' characters...
				v_name_start = v_name_start +1;
			}
			int v_name_end = line.indexOf("\\}");
			if (-1 == v_name_end) {
				System.err.println("Error: no end '#' found for variable name in " +
								"line :" + line + " : Line ignored!");
				return true;
			}
			String v_name = line.substring(v_name_start, v_name_end);
			String v_value = line.substring(v_name_end + 2);
			// if variable was already defined in parent template, warn user
			if (variables.containsKey(v_name)) {
				if (null == template) {
					System.err.println("Warning: template overrides parent template variable " + v_name + " locally with value " + v_value);
				} else {
					System.err.println("Warning: template " + template + " overrides parent template variable " + v_name + " locally with value " + v_value);
				}
			}
			// store variable in map as key, value
			variables.put(v_name, v_value);
			// remove line afterwards
			return true;
		}
		// was a different command, do not remove line afterwards
		return false;
	}
	
	/**
	 * Loops through a specified set of code and replaces variables with their
	 * specified values.
	 *
	 * @param code
	 *      Code lines for which variables should be filled in.
	 * @param variables
	 *      Variables to fill in.
	 */
	private static void fillInVariables(
					ArrayList<String> code,
					HashMap<String, String> variables
	) {
		int lineNumber = -1;
		int size = code.size() - 1;
		while (lineNumber < size){
			lineNumber++;
			String str = code.get(lineNumber);
			// loop through vars, if str contains var key replace with var value
			for (String key : variables.keySet()) {
				String value = variables.get(key);
				str = str.replace(key, value);
			}
			code.set(lineNumber, str);
		}
	}
	
	/**
	 * Loops through the specified code lines and checks for commands. If a line
	 * contains a command the command is executed and the line is replaced with
	 * the result from the command. If a line does not contain a command no
	 * action is taken, the line remains in the specified set of code.
	 * @param template
	 *      Template lines as a list of String objects.
	 * @param parameters
	 *      Map of all parameters specified for this code section.
	 * @param variables
	 *      Map of all variables specified for this code section.
	 */
	private static void parseCommands(
					ArrayList<String> template,
					Map<String, Param> parameters,
					Map<String, String> variables
	) {
		// loop through lines of code
		int lineNumber = -1;
		int size = template.size() - 1;
		while (lineNumber < size){
			lineNumber++;
			// check if line is a command
			CommandResult res = parseCommand(template, lineNumber,
							parameters,	variables);
			if (null != res) {
				// remove specified number of lines
				for (int i = 0; i < res.removed; i++) {
					template.remove(lineNumber);
					size--;
				}
				// add the generated code from command
				if (null != res.added) {
					template.addAll(lineNumber, res.added);
					size += res.added.size();
					lineNumber += res.added.size();
				}
				lineNumber -= res.removed;
				// specified lines removed, nothing added
			}
			// line was not a command, no action performed
		}
	}
	
	/**
	 * Checks a single template line for commands. Returns the set of code
	 * lines produced by the command. Returns an empty set of code lines if the
	 * command did not produce any new code. Returns null if the line contains
	 * no command.
	 *
	 * Prints errors in console if command is badly formatted or unrecognized.
	 *
	 * @param template
	 *      Template in which the command line was found
	 * @param lineNumber
	 *      Line to check for commands.
	 * @param parameters
	 *      Map of all parameters specified for this code section.
	 * @param variables
	 *      Map of all variables specified for this code section.
	 * @return
	 *      Set of code lines produced by the command in the specified line.
	 *      Empty if no lines generated by the command in the specified line.
	 *      Null if no command found in this line.
	 */
	private static CommandResult parseCommand(
					ArrayList<String> template,
					int lineNumber,
					Map<String, Param> parameters,
					Map<String, String> variables
	) {
		String line = template.get(lineNumber);
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
							line + " : Line ignored!");
			return new CommandResult(1, null);
		}
		
		// found command in between $$ command identifiers -> execute command
		String cmd = line.substring(c_start, c_end);
		switch (cmd) {
			case "p-template" :
				return pTemplateCmd(line,lineNumber, parameters, variables);
			case "p-line" :
				return pLineCmd(line, lineNumber, parameters);
			case "template" :
				return templateCmd(line, c_end, parameters, variables);
			case "param" :
				//TODO : could make a command to add a new param type from template
			case "p-block" :
				return pBlockCmd(template, lineNumber, parameters, variables);
			default :
				System.err.println("Error: Unrecognized command : " + cmd);
				return new CommandResult(1, null);
		}
	}
	
	/**
	 * Returns an exact String copy of the leading whitespace used in the
	 * specified line.
	 * @param line
	 *      Line of which leading whitespace should be found.
	 * @return
	 *      Leading whitespace of line.
	 */
	private static String findIndentation(String line) {
		// regex matching anything except whitespace
		Pattern notWhiteSpace = Pattern.compile("\\S");
		// first occurrence of regex
		int indentationEnd = indexOf(notWhiteSpace, line);
		// return part between string start and first non whitespace
		return line.substring(0, indentationEnd);
	}
	
	/**
	 * Returns the index of the first occurrence of the pattern within the
	 * specified String.
	 *
	 * @param pattern
	 *      Pattern to look for.
	 * @param str
	 *      String to search.
	 * @return
	 *      Index of Pattern in String (first occurrence).
	 */
	private static int indexOf(Pattern pattern, String str) {
		Matcher matcher = pattern.matcher(str);
		return matcher.find() ? matcher.start() : -1;
	}
	
	private static String[] findParamList(String line, int lineNumber) {
		// find start of parameter list
		int p_list_start = line.indexOf('[');
		if (-1 == p_list_start) {
			System.err.print("Error: no parameter list provided on line : " +
							lineNumber + " : " + line + " : Line ignored! ");
			System.err.println("Missing opening bracket?");
			return null;
		} else {
			// need to read whats in between list identifiers
			p_list_start = p_list_start + 1;
		}
		// find end of parameter list
		int p_list_end = line.indexOf(']', p_list_start);
		if (-1 == p_list_end) {
			System.err.print("Error: no parameter list provided on line : " +
							lineNumber + " : " + line + " : Line ignored! ");
			System.err.println("Missing closing bracket?");
			return null;
		}
		
		// split on '|' and return list of specified parameters
		return line.substring(p_list_start, p_list_end).split("\\|");
	}
	
	/**
	 * Handles a line containing a $p-block$ command.
	 * @param template
	 *      Template the command was found in as list of Strings.
	 * @param lineNumber
	 *      Line number the command was found in.
	 * @param parameters
	 *      Parameters that were specified for the template containing the
	 *      command.
	 * @return
	 *      A representation of the command result as lines to remove and lines
	 *      to add.
	 */
	private static CommandResult pBlockCmd(
					ArrayList<String> template,
					int lineNumber,
					Map<String, Param> parameters,
					Map<String, String> variables
	) {
		// line at which the p-block command was found
		String line = template.get(lineNumber);
		// find specified list of parameters
		String[] params = findParamList(line, lineNumber);
		// ignore if not found (user is warned)
		if (null == params) {
			return new CommandResult(1, null);
		}
		
		// check if the line contains an opening '{'
		int block_start = -1;
		int start_bracket = line.indexOf("\\{");
		if (-1 == start_bracket) {
			System.err.println("Error: no starting bracket '\\{' for p-block " +
							"command" +
							" " +
							" : " + line + " on line : " + lineNumber + " : Line ignored!");
			//TODO: assuming start bracket is on same line
			return new CommandResult(1, null);
		} else {
			// block starts is at lineNumber
			block_start = lineNumber;
		}
		
		// find end bracket
		int block_end = -1;
		int block_line = block_start + 1;
		int size = template.size();
		while (block_line < size) {
			block_line++;
			String currentLine = template.get(block_line);
			int end_bracket = currentLine.indexOf("\\}");
			if (-1 != end_bracket) {
				block_end = block_line;
				break;
			}
		}
		
		if (-1 == block_end) {
			System.err.println("Error: no end bracket '\\}' for p-block command " +
							" : " + line + " on line : " + lineNumber + " : Line ignored!");
			return new CommandResult(1, null);
		}
		
		//TODO: assuming end bracket is on it's own line
		
		ArrayList<String> result = new ArrayList<>();
		
		int startIndex = block_start + 1;
		int endIndex = block_end;
		
		// get parameter code block from template
		ArrayList<String> code = new ArrayList<>(template.subList(startIndex,
						endIndex));
		
		// if keyword all is used in list -> fill template for all params
		for (String p : params) {
			if (p.equals("all")) {
				// process parameter in order of id
				ArrayList<Param> ps = new ArrayList<>();
				parameters.forEach((name, par) -> {
					ps.add(par);
				});
				ArrayList<Param> sorted = sortParams(ps);
				sorted.forEach(par -> {
					// make local copy so parent variables aren't modified
					HashMap<String, String> vars = new HashMap<>(variables);
					ArrayList<String> pCode = new ArrayList<>(code);
					// add param vars and process
					addParamVariables(vars, par);
					ArrayList<String> blockLines = processTemplate(null, pCode, parameters,
									vars);
					result.addAll(blockLines);
				});
				return new CommandResult((block_end + 1 - block_start), result);
			}
		}
		// else only fill template for specified parameters
		for (String p : params) {
			// fill in the template
			Param par = parameters.get(p);
			if (null == par) {
				System.err.println("Error: Unknown parameter : " + p + " : Parameter " +
								"skipped!");
				continue;
			}
			// make local copy so parent variables aren't modified
			HashMap<String, String> vars = new HashMap<>(variables);
			ArrayList<String> pCode = new ArrayList<>(code);
			// add param vars and process
			addParamVariables(vars, par);
			ArrayList<String> blockLines = processTemplate(null, pCode, parameters,
							vars);
			result.addAll(blockLines);
		}
		return new CommandResult((block_end + 1 - block_start), result);
	}
	
	/**
	 * Adds the parameter keywords as variables to be filled in.
	 * @param vars
	 *      Set of variables to add parameter keywords to.
	 * @param param
	 *      Parameter from which the keywords should be used.
	 */
	private static void addParamVariables(HashMap<String, String> vars, Param param) {
		vars.put("p#name", param.name);
		vars.put("p#id", Integer.toString(param.id));
		vars.put("p#enumName", param.enumName);
		vars.put("p#dataType", param.dataType);
		vars.put("p#defaultValue", param.defaultValue);
	}
	
	private static CommandResult pTemplateCmd(
					String line,
					int lineNumber,
					Map<String, Param> parameters,
					Map<String, String> variables
	) {
		// find indentation used
		String indent = findIndentation(line);
		// find specified list of parameters
		String[] params = findParamList(line, lineNumber);
		// ignore if not found (user is warned)
		if (null == params) {
			return new CommandResult(1, null);
		}
		
		int p_list_end = line.indexOf(']');
		// try to find specified template filename
		int tmp_start = p_list_end + 2;
		if (tmp_start >= line.length()) {
			System.err.println("Error: No template name specified for template " +
							"command : " + line + " : Line ignored!");
			return new CommandResult(1, null);
		}
		int tmp_end = line.indexOf(TEMPLATE_EXTENSION, tmp_start);
		if (tmp_end == -1) {
			System.err.println("Error: Template extension missing for template " +
							"command : " + line + " : attempting with appended extension!");
			return new CommandResult(1, null);
		} else {
			// need to include extension
			tmp_end = tmp_end + EXTENSION_LENGTH;
		}
		// template specified
		String template = line.substring(tmp_start, tmp_end);
		
		ArrayList<String> lines = new ArrayList<>();
		
		boolean foundAllKeyword = false;
		// if keyword all is used in list -> fill template for all params
		for (String p : params) {
			if (p.equals("all")) {
				foundAllKeyword = true;
				// process parameter in order of id
				ArrayList<Param> ps = new ArrayList<>();
				parameters.forEach((name, par) -> {
					ps.add(par);
				});
				ArrayList<Param> sorted = sortParams(ps);
				sorted.forEach(par -> {
					// make local copy so parent variables aren't modified
					HashMap<String, String> vars = new HashMap<>(variables);
					// add param vars and process
					addParamVariables(vars, par);
					ArrayList<String> newLines = processTemplate(template, parameters,
									vars);
					checkParamTemplateResult(lines, newLines, template, par);
				});
				break;
			}
		}
		if (!foundAllKeyword) {
			// only fill template for specified parameters
			for (String p : params) {
				// fill in the template
				Param par = parameters.get(p);
				if (null == par) {
					System.err.println("Error: Unknown parameter : " + p + " : Parameter " +
									"skipped!");
					continue;
				}
				// make local copy so parent variables aren't modified
				HashMap<String, String> vars = new HashMap<>(variables);
				// add param vars and process
				addParamVariables(vars, par);
				ArrayList<String> newLines = processTemplate(template, parameters,
								vars);
				checkParamTemplateResult(lines, newLines, template, par);
			}
		}
		if (continue_indentation) {
			addPrefix(lines, indent);
		}
		return new CommandResult(1, lines);
	}
	
	/**
	 * Adds the specified prefix string to each line in the specified set of
	 * lines.
	 * @param lines
	 *      Lines that should have a prefix.
	 * @param prefix
	 *      Prefix to add for each line.
	 */
	private static void addPrefix(ArrayList<String> lines, String prefix) {
		if (null == lines || null == prefix) {
			return;
		}
		int size = lines.size();
		for (int i = 0; i < size; i++) {
			String line = lines.get(i);
			line = prefix + line;
			lines.set(i, line);
		}
	}
	
	/**
	 * Handles a p-line command. Displays a warning if no parameter list is
	 * provided. The content after the specified parameter list is added once
	 * for each specified parameter in the list. For each specific parameter
	 * keywords in the content line are replaced with the parameter's values.
	 * @param line
	 *      Line containing a $template$ command
	 * @param parameters
	 *      Parameters specified for template containing this template command.
	 * @return
	 *      Command result specifying lines to remove and lines
	 */
	private static CommandResult pLineCmd(
					String line,
					int lineNumber,
					Map<String, Param> parameters
	) {
		// find specified list of parameters
		String[] params = findParamList(line, lineNumber);
		// ignore if not found (user is warned)
		if (null == params) {
			return new CommandResult(1, null);
		}
		
		int p_list_end = line.indexOf(']');
		String paramLine = line.substring(p_list_end + 2);
		
		ArrayList<String> lines = new ArrayList<>();
		// if keyword all is used in list -> fill line for all params
		for (String p : params) {
			if (p.equals("all")) {
				// process parameter in order of id
				ArrayList<Param> ps = new ArrayList<>();
				parameters.forEach((name, par) -> {
					ps.add(par);
				});
				ArrayList<Param> sorted = sortParams(ps);
				sorted.forEach(par -> {
					// fill in the code line with values of par
					lines.add(fillInParam(paramLine, par));
				});
				return new CommandResult(1, lines);
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
		return new CommandResult(1, lines);
	}
	
	/**
	 * Handles a line containing a $template$ command. Displays a warning if
	 * the $template$ command is not followed by a template name. If a template
	 * name is specified it will be processed with the specified parent
	 * template parameters and variables and the output code generated from it
	 * will be returned.
	 *
	 * If a specific parameter was specified as param, it's values will be
	 * filled in for any parameter keywords in the specified template.
	 * @param line
	 *      Line containing a $template$ command
	 * @param index
	 *      Index of closing $ of $template$ command identifier in line.
	 * @param parameters
	 *      Parameters specified for template containing this template command.
	 * @param variables
	 *      Variables specified for template containing this template command.
	 * @return
	 *      Output code produced from the template as a List of String objects.
	 */
	private static CommandResult templateCmd(
					String line,
					int index,
					Map<String, Param> parameters,
					Map<String, String> variables
	) {
		// find indentation used
		String indent = findIndentation(line);
		// find specified template in line
		int tmp_start = index + 2;
		if (tmp_start >= line.length()) {
			System.err.println("Error: No template name specified for template " +
							"command : " + line + " : Line ignored!");
			return new CommandResult(1, null);
		}
		int tmp_end = line.indexOf(TEMPLATE_EXTENSION, tmp_start);
		if (tmp_end == -1) {
			System.err.println("Error: Template extension missing for template " +
							"command : " + line + " : Line ignored!");
			return new CommandResult(1, null);
		} else {
			// need to include extension part
			tmp_end = tmp_end + EXTENSION_LENGTH;
		}
		String template = line.substring(tmp_start, tmp_end);
		
		// process template
		ArrayList<String> newLines = processTemplate(template, parameters,
						variables);
		if (continue_indentation) {
			addPrefix(newLines, indent);
		}
		
		if (null == newLines) {
			// template not found, user is already warned, return empty
			return new CommandResult(1, null);
		} else {
			// template found, return result from template
			return new CommandResult(1, newLines);
		}
	}
	
	/**
	 * Checks if filling in a template for a specific parameter was successful.
	 * - If the template existed and wasn't empty, the lines generated from
	 * it are are added to the output.
	 * - If the template was empty, a warning is displayed and a comment is
	 * included in the output as an indication for the user where the parameter
	 * specific code should have been.
	 * - If the template for that specific parameter did not exist, a blank
	 * template is created at it's location. A warning is displayed that this
	 * blank template was created and should be filled in by the user. A comment
	 * is included in the output as an indication for the user where the
	 * parameter specific code should have been.
	 *
	 * @param lines
	 *      Output lines as a list of String objects.
	 * @param newLines
	 *      Set of lines generated from the template
	 * @param template
	 *      Template file used for generating lines
	 * @param param
	 *      Parameter whose values were used to fill in template.
	 * @return
	 *      Set of output lines as a list of String objects.
	 */
	private static CommandResult checkParamTemplateResult(
					ArrayList<String> lines,
					ArrayList<String> newLines,
					String template,
					Param param
	) {
		if (null != newLines && 0 == newLines.size()) {
			// template was found but was empty, warn user
			System.out.println("Warning: template " + param.name + "/" + template +
							" was empty!" +
							" A comment line has been added in the output to indicate" +
							" where the code for this template could be added.");
			lines.add("// Add " + param.name + " code section here!");
		} else if (null != newLines) {
			// template was found and was not empty, add result to output
			lines.addAll(newLines);
		} else {
			// template was not found, warn user and make blank template
			System.out.println("Warning: the template " + param.name + "/" +
							template + " was missing!" +
							" A blank template has been created at this location\n" +
							"A comment line has been added in the output to indicate" +
							" where the code for this template could be added.");
			Utilities.writeLinesToFile(
							null,
							TEMPLATE_DIR + "/" + template, overwrite_existing);
			
			lines.add("// Add " + param.name + " code section here!");
		}
		return new CommandResult(1, lines);
	}
	
	/**
	 * Fills in the values of the specified parameter for the parameter
	 * keywords in the specified line.
	 * Defined parameter keywords that get replaced with values are:
	 *  - p#name : replaced with name of parameter
	 *  - p#id : replaced with enum integer identifier of parameter
	 *  - p#enumName : replaced with name + '_param_id' suffix
	 *  - p#dataType : replaced with data type of parameter
	 *  - p#defaultValue : replaced with default value of parameter
	 *
	 * @param line
	 *      Line String where parameter values should be filled in.
	 * @param param
	 *      Parameter whose values should be filled into specified line.
	 * @return
	 *      Line String with parameter keywords replaced with the values from
	 *      the specified parameter.
	 */
	private static String fillInParam(String line, Param param) {
		// replace parameter keywords with param values
		line = line.replace("p#name", param.name);
		line = line.replace("p#id", Integer.toString(param.id));
		line = line.replace("p#enumName", param.enumName);
		line = line.replace("p#dataType", param.dataType);
		line = line.replace("p#defaultValue", param.defaultValue);
		
		return line;
	}
	
	/**
	 * Sorts a list of parameters based on their ids. Uses a simple top down
	 * merge sort.
	 * @param params
	 *      List of parameters to sort.
	 * @return
	 *      Sorted list of parameters.
	 */
	private static ArrayList<Param> sortParams(ArrayList<Param> params) {
		if (null == params) {
			return null;
		}
		int size = params.size();
		if (1 >= size) {
			return params;
		}
		
		ArrayList<Param> left = new ArrayList<>();
		ArrayList<Param> right = new ArrayList<>();
		
		for (int i = 0; i < size; i++) {
			if (i < size/2) {
				left.add(params.get(i));
			} else {
				right.add(params.get(i));
			}
		}
		
		ArrayList<Param> left_sort = sortParams(left);
		ArrayList<Param> right_sort = sortParams(right);
		
		return mergeParams(left_sort, right_sort);
	}
	
	/**
	 * Merges two lists of parameters based on their ids.
	 * @param left
	 *      First list.
	 * @param right
	 *      Second List.
	 * @return
	 *      Merged result of the two lists.
	 */
	private static ArrayList<Param> mergeParams(ArrayList<Param> left,
	                                            ArrayList<Param> right) {
		ArrayList<Param> res = new ArrayList<>();
		
		while (!left.isEmpty() && !right.isEmpty()) {
			if (left.get(0).id <= right.get(0).id) {
				res.add(left.remove(0));
			} else {
				res.add(right.remove(0));
			}
		}
		while (!left.isEmpty()) {
			res.add(left.remove(0));
		}
		while (!right.isEmpty()) {
			res.add(right.remove(0));
		}
		return res;
	}

	/**
	 * Removes template comment lines from the code. Template comments are
	 * identified as lines starting with '//<', possibly preceded by whitespace.
	 *
	 * @param code
	 *      Code represented as a list of Strings.
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
			// remove if first non-whitespace chars equal template comment identifier
			if (trimStr.length() > 2 && trimStr.substring(0,3).equals("//<")) {
				itr.remove();
			}
		}
	}
}
