import java.io.*;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import org.json.JSONArray;
import org.json.JSONObject;

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
	
	private static final String TEMPLATE_EXTENSION = ".cgen_template";
	private static final int EXTENSION_LENGTH = 14;
	private static final String TEMPLATE_DIR = "templates/";
	private static final String SETTINGS_FILE = "settings.json";
	private static final String DEFAULT_PARAMS = "params.csv";
	private static JSONObject settings;

	public static void main(String[] args) {
		String intro =
					"\nThis is the PQ9_bus_software subsystem code generator!\n" +
					"Code generator settings can be found in settings.json.\n" +
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
									((JSONObject) file).getString("filename") +
													TEMPLATE_EXTENSION);
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
						ArrayList<Param> pars = Utilities.readParamCSV((String) parameters);
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
					// no file specific params specified, use default params.csv
					ArrayList<Param> pars = Utilities.readParamCSV(DEFAULT_PARAMS);
					pars.forEach(p -> filePars.put(p.name, p));
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
			System.out.println("\nProcessing file : " + fileName);
			Map<String, Param> pars = fileParams.get(fileName);
			ArrayList<String> codeLines = processTemplate(baseTemplate, pars);
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
		return processTemplate(templateFile, parameters, vars, null);
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
					Map<String, String> variables,
					Param param
	) {
		// make local copy so parent variables aren't modified
		HashMap<String, String> vars = new HashMap<>(variables);
		
		// read code from template file
		ArrayList<String> code = Utilities.readLinesFromFile(TEMPLATE_DIR + templateFile);
		if (null == code) {
			// nothing read from file, user is already warned
			return null;
		}
		
		// remove comments beforehand just in case to prevent misinterpretation
		removeTemplateComments(code);
		
		// check if templateFile specifies additional variables
		parseVariables(templateFile, code, vars);
		
		// loop through code lines, replace all var keys with var values
		fillInVariables(code, vars);
		
		// If template is being processed for a specific param, fill in it's values
		if (null != param) {
			fillInParam(code, param);
		}
		
		// check for commands
		parseCommands(code, parameters, vars, param);
		
		return code;
	}
	
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
			int v_name_start = line.indexOf('#', c_end);
			if (-1 == v_name_start) {
				// no starting '#', no name specified
				System.err.println("Error: no starting '#' found for variable name " +
								"in line :" + line + " : Line ignored!");
				return true;
			} else {
				// need to parse what's in between '#' characters...
				v_name_start = v_name_start +1;
			}
			int v_name_end = line.indexOf('#', v_name_start);
			if (-1 == v_name_end) {
				System.err.println("Error: no end '#' found for variable name in " +
								"line :" + line + " : Line ignored!");
				return true;
			}
			String v_name = line.substring(v_name_start, v_name_end);
			String v_value = line.substring(v_name_end + 2);
			// if variable was already defined in parent template, warn user
			if (variables.containsKey(v_name)) {
				System.err.println("Warning: template " + template +
								" overrides parent template variable " + v_name +
								" locally with value " + v_value);
			}
			// store variable in map as key, value
			variables.put(v_name, v_value);
			// remove line afterwards
			return true;
		}
		// was a different command, do not remove line afterwards
		return false;
	}
	
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
	 * @param param
	 *      Optional parameter whose values should be filled in for this code
	 *      section. Null if not specified.
	 */
	private static void parseCommands(
					ArrayList<String> template,
					Map<String, Param> parameters,
					Map<String, String> variables,
					Param param
	) {
		// loop through lines of code
		int lineNumber = -1;
		int size = template.size() - 1;
		while (lineNumber < size){
			lineNumber++;
			// check if line is a command
			CommandResult res = parseCommand(template, lineNumber,
							parameters,	variables,
							param);
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
				}
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
	 * @param param
	 *      Optional parameter whose values should be filled in for this code
	 *      section. Null if not specified.
	 * @return
	 *      Set of code lines produced by the command in the specified line.
	 *      Empty if no lines generated by the command in the specified line.
	 *      Null if no command found in this line.
	 */
	private static CommandResult parseCommand(
					ArrayList<String> template,
					int lineNumber,
					Map<String, Param> parameters,
					Map<String, String> variables,
					Param param
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
				return pTemplateCmd(line, c_end, parameters, variables);
			case "p-line" :
				return pLineCmd(line, c_end, parameters);
			case "template" :
				return templateCmd(line, c_end, parameters, variables, param);
			case "param" :
				//TODO : could make a command to add a new param type from template
			case "p-block" :
				return pBlockCmd(template, lineNumber, c_end, parameters);
			default :
				System.err.println("Error: Unrecognized command : " + cmd);
				return new CommandResult(1, null);
		}
	}
	
	/**
	 * Handles a line containing a $p-block$ command.
	 * @param template
	 *      Template the command was found in as list of Strings.
	 * @param lineNumber
	 *      Line number the command was found in.
	 * @param c_end
	 *      Index of the closing $ command identifier in the command line.
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
					int c_end,
					Map<String, Param> parameters
	) {
		// line at which the p-block command was found
		String line = template.get(lineNumber);
		
		// find start of parameter list
		int p_list_start = line.indexOf('[', c_end);
		if (-1 == p_list_start) {
			System.err.println("Error: no parameter list provided for p-block " +
							"command : " + line + " on line : " + lineNumber +
							" : Line ignored!");
			return new CommandResult(1, null);
		} else {
			// need to read whats in between list identifiers
			p_list_start = p_list_start + 1;
		}
		// find end of parameter list
		int p_list_end = line.indexOf(']', p_list_start);
		if (-1 == p_list_end) {
			System.err.println("Error: badly formatted parameter list provided " +
							"for p-block command : " + line + " on line : " + lineNumber +
							" : Line ignored!");
			return new CommandResult(1, null);
		}
		
		// check if the line contains an opening '{'
		int block_start = -1;
		int start_bracket = line.indexOf("\\{", p_list_end);
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
		
		// read set of specified parameters
		String[] params = line.substring(p_list_start, p_list_end).split("\\|");
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
					ArrayList<String> blockLines =
									new ArrayList<>(template.subList(startIndex, endIndex));
					// replace parameter keywords
					fillInParam(blockLines, par);
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
			ArrayList<String> blockLines =
							new ArrayList<>(template.subList(startIndex, endIndex));
			// replace parameter keywords
			fillInParam(blockLines, par);
			result.addAll(blockLines);
		}
		return new CommandResult((block_end + 1 - block_start), result);
	}
	
	private static CommandResult pTemplateCmd(
					String line,
					int index,
					Map<String, Param> parameters,
					Map<String, String> variables
	) {
		ArrayList<String> lines = new ArrayList<>();
		int p_list_start = line.indexOf('[', index);
		if (-1 == p_list_start) {
			System.err.println("Error: no parameter list provided for p-line " +
							"command : " + line + " : Line ignored!");
			return new CommandResult(1, null);
		} else {
			// need to read whats in between list identifiers
			p_list_start = p_list_start + 1;
		}
		int p_list_end = line.indexOf(']', index);
		if (-1 == p_list_end) {
			System.err.println("Error: badly formatted parameter list provided " +
							"for p-line command : " + line + " : Line ignored!");
			return new CommandResult(1, null);
		}
		// have a template to fill in for a list of parameters
		String[] params = line.substring(p_list_start, p_list_end).split("\\|");
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
					addParLines(lines, line, p_list_end, par,	parameters,	variables);
				});
				return new CommandResult(1, lines);
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
			addParLines(lines, line, p_list_end, par, parameters, variables);
		}
		return new CommandResult(1, lines);
	}
	
	private static void addParLines(
					ArrayList<String> lines,
					String line,
					int p_list_end,
					Param param,
					Map<String, Param> parameters,
					Map<String, String> variables
	) {
		int tmp_start = p_list_end + 2;
		if (tmp_start >= line.length()) {
			System.err.println("Error: No template name specified for template " +
							"command : " + line + " : Line ignored!");
			return;
		}
		int tmp_end = line.indexOf(TEMPLATE_EXTENSION, tmp_start);
		if (tmp_end == -1) {
			System.err.println("Error: Template extension missing for template " +
							"command : " + line + " : Line ignored!");
			return;
		} else {
			// need to include extension
			tmp_end = tmp_end + EXTENSION_LENGTH;
		}
		String template = line.substring(tmp_start, tmp_end);
		template = fillInParam(template, param);
		// fill in the template with values of par
		ArrayList<String> newLines = processTemplate(template, parameters,
						variables, param);
		checkParamTemplateResult(lines, newLines, template, param);
	}
	
	/**
	 * Handles a p-line command. Displays a warning if no parameter list is
	 * provided. The content after the specified parameter list is added once
	 * for each specified parameter in the list. For each specific parameter
	 * keywords in the content line are replaced with the parameter's values.
	 * @param line
	 *      Line containing a $template$ command
	 * @param index
	 *      Index of closing $ of $template$ command identifier in line.
	 * @param parameters
	 *      Parameters specified for template containing this template command.
	 * @return
	 *      Command result specifying lines to remove and lines
	 */
	private static CommandResult pLineCmd(
					String line,
					int index,
					Map<String, Param> parameters
	) {
		ArrayList<String> lines = new ArrayList<>();
		int p_list_start = line.indexOf('[', index);
		if (-1 == p_list_start) {
			System.err.println("Error: no parameter list provided for p-line " +
							"command : " + line + " : Line ignored!");
			return new CommandResult(1, null);
		} else {
			// need to read whats in between list identifiers
			p_list_start = p_list_start + 1;
		}
		int p_list_end = line.indexOf(']', p_list_start);
		if (-1 == p_list_end) {
			System.err.println("Error: badly formatted parameter list provided " +
							"for p-line command : " + line + " : Line ignored!");
			return new CommandResult(1, null);
		}
		// have a code line to fill in for a list of parameters
		String[] params = line.substring(p_list_start, p_list_end).split("\\|");
		String paramLine = line.substring(p_list_end + 2);
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
	 * @param param
	 *      Parameter whose values should be filled into the specified template.
	 * @return
	 *      Output code produced from the template as a List of String objects.
	 */
	private static CommandResult templateCmd(
					String line,
					int index,
					Map<String, Param> parameters,
					Map<String, String> variables,
					Param param
	) {
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
						variables, param);
		if (null == param) {
			// template was not processed for a specific param
			if (null == newLines) {
				// template not found, user is already warned, return empty
				return new CommandResult(1, null);
			} else {
				// template found, return result from template
				return new CommandResult(1, newLines);
			}
		} else {
			// check if processing template for a specific param went correctly
			ArrayList<String> lines = new ArrayList<>();
			return checkParamTemplateResult(lines, newLines, template, param);
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
							TEMPLATE_DIR + "/" + template,
							overwriteExisting);
			
			lines.add("// Add " + param.name + " code section here!");
		}
		return new CommandResult(1, lines);
	}
	
	/**
	 * Fills in the values of the specified parameter for the parameter
	 * keywords in the specified set of template lines.
	 * Defined parameter keywords that get replaced with values are:
	 *  - p#name : replaced with name of parameter
	 *  - p#id : replaced with enum integer identifier of parameter
	 *  - p#enumName : replaced with name + '_param_id' suffix
	 *  - p#dataType : replaced with data type of parameter
	 *  - p#defaultValue : replaced with default value of parameter
	 *
	 * @param lines
	 *      Template lines as a list of String objects.
	 * @param param
	 *      Parameter whose values should be filled into the specified lines.
	 */
	private static void fillInParam(ArrayList<String> lines, Param param) {
		if (null == lines) {
			return;
		}
		// loop through code lines
		int lineNumber = -1;
		int size = lines.size() - 1;
		while (lineNumber < size){
			lineNumber++;
			String line = lines.get(lineNumber);
			// fill in parameter values for current line
			line = fillInParam(line, param);
			lines.set(lineNumber, line);
		}
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
