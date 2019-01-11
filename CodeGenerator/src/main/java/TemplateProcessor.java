import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Includes template processing functions
 */
public class TemplateProcessor {
	private String subSysName;
	private String templateDir;
	private String templateExtension;
	private int extensionLength;
	private boolean continue_indentation;
	private boolean overwrite_existing;
	
	public TemplateProcessor(
					String subSysName,
					String templateDir,
					String templateExtension,
					boolean continue_indentation,
					boolean overwrite_existing
	) {
		this.subSysName = subSysName;
		this.templateDir = templateDir;
		this.templateExtension = templateExtension;
		extensionLength = templateExtension.length();
		this.continue_indentation = continue_indentation;
		this.overwrite_existing = overwrite_existing;
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
	public ArrayList<String> processTemplate(
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
	private ArrayList<String> processTemplate(
					String templateFile,
					Map<String, Param> parameters,
					Map<String, String> variables
	) {
		// read code from template file
		ArrayList<String> code = Utilities.readLinesFromFile(templateDir + templateFile);
		
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
	private ArrayList<String> processTemplate(
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
	private void parseVariables(
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
	private boolean parseVariable(
					String line,
					String template,
					Map<String, String> variables
	) {
		// try to find $command$ section in provided line
		if (null == line) {
			Utilities.log("Error: Line to check for command was null!");
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
			Utilities.log("Error: No end '$' found for command in line: " +
							line + " : Line ignored!");
			return true;
		}
		
		// found command in between $$ command identifiers -> execute command
		String cmd = line.substring(c_start, c_end);
		if (cmd.equals("var")) {
			int v_name_start = line.indexOf("\\{");
			if (-1 == v_name_start) {
				// no starting '#', no name specified
				Utilities.log("Error: no starting '#' found for variable name " +
								"in line :" + line + " : Line ignored!");
				return true;
			} else {
				// need to parse what's in between '#' characters...
				v_name_start = v_name_start +1;
			}
			int v_name_end = line.indexOf("\\}");
			if (-1 == v_name_end) {
				Utilities.log("Error: no end '#' found for variable name in " +
								"line :" + line + " : Line ignored!");
				return true;
			}
			String v_name = line.substring(v_name_start, v_name_end);
			String v_value = line.substring(v_name_end + 2);
			// if variable was already defined in parent template, warn user
			if (variables.containsKey(v_name)) {
				if (null == template) {
					Utilities.log("Warning: template overrides parent template variable " + v_name + " locally with value " + v_value);
				} else {
					Utilities.log("Warning: template " + template + " overrides parent template variable " + v_name + " locally with value " + v_value);
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
	private void fillInVariables(
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
	private void parseCommands(
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
	private CommandResult parseCommand(
					ArrayList<String> template,
					int lineNumber,
					Map<String, Param> parameters,
					Map<String, String> variables
	) {
		String line = template.get(lineNumber);
		// try to find $command$ section in provided line
		if (null == line) {
			Utilities.log("Error: Line to check for command was null!");
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
			Utilities.log("Error: No end '$' found for command in line: " +
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
				Utilities.log("Error: Unrecognized command : " + cmd);
				return new CommandResult(1, null);
		}
	}
	
	private String[] findParamList(String line, int lineNumber) {
		// find start of parameter list
		int p_list_start = line.indexOf('[');
		if (-1 == p_list_start) {
			System.out.print("Error: no parameter list provided on line : " +
							lineNumber + " : " + line + " : Line ignored! ");
			Utilities.log("Missing opening bracket?");
			return null;
		} else {
			// need to read whats in between list identifiers
			p_list_start = p_list_start + 1;
		}
		// find end of parameter list
		int p_list_end = line.indexOf(']', p_list_start);
		if (-1 == p_list_end) {
			System.out.print("Error: no parameter list provided on line : " +
							lineNumber + " : " + line + " : Line ignored! ");
			Utilities.log("Missing closing bracket?");
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
	private CommandResult pBlockCmd(
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
			Utilities.log("Error: no starting bracket '\\{' for p-block " +
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
			Utilities.log("Error: no end bracket '\\}' for p-block command " +
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
				ArrayList<Param> sorted = Param.sortParams(ps);
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
				Utilities.log("Error: Unknown parameter : " + p + " : Parameter " +
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
	private void addParamVariables(HashMap<String, String> vars, Param param) {
		vars.put("p#name", param.name);
		vars.put("p#id", Integer.toString(param.id));
		vars.put("p#enumName", param.enumName);
		vars.put("p#dataType", param.dataType);
		vars.put("p#defaultValue", param.defaultValue);
		vars.put("p#dType", param.dType);
		vars.put("p#hexId", param.hexId);
	}
	
	private CommandResult pTemplateCmd(
					String line,
					int lineNumber,
					Map<String, Param> parameters,
					Map<String, String> variables
	) {
		// find indentation used
		String indent = Utilities.findIndentation(line);
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
			Utilities.log("Error: No template name specified for template " +
							"command : " + line + " : Line ignored!");
			return new CommandResult(1, null);
		}
		int tmp_end = line.indexOf(templateExtension, tmp_start);
		if (tmp_end == -1) {
			Utilities.log("Error: Template extension missing for template " +
							"command : " + line + " : attempting with appended extension!");
			return new CommandResult(1, null);
		} else {
			// need to include extension
			tmp_end = tmp_end + extensionLength;
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
				ArrayList<Param> sorted = Param.sortParams(ps);
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
					Utilities.log("Error: Unknown parameter : " + p + " : Parameter " +
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
	private void addPrefix(ArrayList<String> lines, String prefix) {
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
	private CommandResult pLineCmd(
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
				ArrayList<Param> sorted = Param.sortParams(ps);
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
				Utilities.log("Error: Unknown parameter : " + p + " : Parameter " +
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
	private CommandResult templateCmd(
					String line,
					int index,
					Map<String, Param> parameters,
					Map<String, String> variables
	) {
		// find indentation used
		String indent = Utilities.findIndentation(line);
		// find specified template in line
		int tmp_start = index + 2;
		if (tmp_start >= line.length()) {
			Utilities.log("Error: No template name specified for template " +
							"command : " + line + " : Line ignored!");
			return new CommandResult(1, null);
		}
		int tmp_end = line.indexOf(templateExtension, tmp_start);
		if (tmp_end == -1) {
			Utilities.log("Error: Template extension missing for template " +
							"command : " + line + " : Line ignored!");
			return new CommandResult(1, null);
		} else {
			// need to include extension part
			tmp_end = tmp_end + extensionLength;
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
	private CommandResult checkParamTemplateResult(
					ArrayList<String> lines,
					ArrayList<String> newLines,
					String template,
					Param param
	) {
		if (null != newLines && 0 == newLines.size()) {
			// template was found but was empty, warn user
			Utilities.log("Warning: template " + param.name + "/" + template +
							" was empty!" +
							" A comment line has been added in the output to indicate" +
							" where the code for this template could be added.");
			lines.add("// Add " + param.name + " code section here!");
		} else if (null != newLines) {
			// template was found and was not empty, add result to output
			lines.addAll(newLines);
		} else {
			// template was not found, warn user and make blank template
			Utilities.log("Warning: the template " + param.name + "/" +
							template + " was missing!" +
							" A blank template has been created at this location\n" +
							"A comment line has been added in the output to indicate" +
							" where the code for this template could be added.");
			Utilities.writeLinesToFile(
							null,
							templateDir + "/" + template, overwrite_existing);
			
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
	private String fillInParam(String line, Param param) {
		// replace parameter keywords with param values
		line = line.replace("p#name", param.name);
		line = line.replace("p#id", Integer.toString(param.id));
		line = line.replace("p#enumName", param.enumName);
		line = line.replace("p#dataType", param.dataType);
		line = line.replace("p#defaultValue", param.defaultValue);
		line = line.replace("p#dType", param.dType);
		line = line.replace("p#hexId", param.hexId);
		
		return line;
	}
	
	/**
	 * Removes template comment lines from the code. Template comments are
	 * identified as lines starting with '//<', possibly preceded by whitespace.
	 *
	 * @param code
	 *      Code represented as a list of Strings.
	 */
	private void removeTemplateComments(ArrayList<String> code) {
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
