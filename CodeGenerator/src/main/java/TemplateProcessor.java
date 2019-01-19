import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Includes template processing functions
 *
 * To add additional template commands, see the ParseCommand function and it's
 * currently implemented commands.
 *
 * To change the format used to identify commands and variables, modify the
 * regex patterns listen in the constructor just below.
 *
 * To change the available parameter keywords and how they're substituted, see
 * the Param.java class.
 */
public class TemplateProcessor {
	private String subSysName;
	private String templateDir;
	private String templateExtension;
	private boolean continue_indentation;
	private boolean overwrite_existing;
	
	// define regex pattern variables
	private String commandIdentifier;
	private Pattern cmdPattern;
	private Pattern varNamePattern;
	private Pattern paramListPattern;
	private String paramListDelimiter;
	private Pattern templateNamePattern;
	private Pattern templateCommentPattern;
	
	/**
	 * Constructs a template processor with the specified settings. Helps to
	 * organize the program by moving all related functionality here.
	 * @param subSysName
	 *      Name of subsystem being created.
	 * @param templateDir
	 *      Name of template directory.
	 * @param templateExtension
	 *      Extension used for templates in regex format.
	 * @param continue_indentation
	 *      Whether or not automatic indentation was enabled.
	 * @param overwrite_existing
	 *      Whether or not overwriting of existing files was enabled.
	 */
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
		this.continue_indentation = continue_indentation;
		this.overwrite_existing = overwrite_existing;
		
		/*
		 * This section specifies the regex patterns used for templates.
		 * - The enclosing identifiers for template commands : $ and $
		 * - The enclosing identifiers for variable names : \{ and \}
		 * - The identifiers for template file and parameter list arguments required
		 *   for certain commands.
		 * - The template comment identifier after whitespace : \\<
		 *
		 * They are specified separately here to make them easy to change, should you
		 * need a different format for your application.
		 *
		 * //TODO: not yet applied for p-block due to it spanning multiple lines
		 */
		
		// Be mindful when changing these that java requires an additional escape
		// slash for certain regex operators. And regex requires one for certain
		// opening operators only : \{ \[, but not necessarily for closing : } ]
		
		// command name regex pattern : anything between $$
		commandIdentifier = "\\$";
		String escOpenbrace = "\\\\\\{";
		String escCloseBrace = "\\\\}";
		cmdPattern = Pattern.compile("(?<=" + commandIdentifier + ").*(?=" + commandIdentifier + ")");
		// variable name regex pattern : anything between \{ and \}
		varNamePattern = Pattern.compile("(?<="+ escOpenbrace +").*(?="+ escCloseBrace +")");
		// parameter list regex pattern : anything between [ and ]
		paramListPattern = Pattern.compile("(?<=\\[).*(?=])");
		// regex pattern to identify list entries : |
		paramListDelimiter = "\\|";
		// template filename regex pattern
		templateNamePattern = Pattern.compile("(?<= ).*" + templateExtension);
		// template comment regex pattern : \\< preceded by tabs or spaces
		templateCommentPattern = Pattern.compile("[\\t ]*//<");
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
	ArrayList<String> processTemplate(
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
		
		// check for commands
		parseCommands(code, templateFile, parameters, vars);
		
		return code;
	}
	
	/**
	 * Loops through the specified code lines and checks for commands. If a line
	 * contains a command the command is executed and the line is replaced with
	 * the result from the command. If a line does not contain a command no
	 * action is taken, the line remains in the specified set of code.
	 * @param template
	 *      Template lines as a list of String objects.
	 * @param templateFile
	 *      The template filename that is currently being processed.
	 * @param parameters
	 *      Map of all parameters specified for this code section.
	 * @param variables
	 *      Map of all variables specified for this code section.
	 */
	private void parseCommands(
					ArrayList<String> template,
					String templateFile,
					Map<String, Param> parameters,
					Map<String, String> variables
	) {
		// loop through lines of code
		int lineNumber = -1;
		int size = template.size() - 1;
		while (lineNumber < size){
			lineNumber++;
			// fill in variables
			String str = template.get(lineNumber);
			
			//TODO : should probably disallow '$' in variable definitions
			// Make sure commands (anything between $$) remain unchanged by storing
			// the original command in the line
			RegexResult cmdRR = Utilities.firstMatch(str, cmdPattern, 0);
			
			// loop through vars, if str contains var key replace with var value
			for (String key : variables.keySet()) {
				String value = variables.get(key);
				str = str.replace(key, value);
			}
			
			// restore the original command after filling in variables
			RegexResult newCmdRR = Utilities.firstMatch(str, cmdPattern, 0);
			if (null != cmdRR && null != newCmdRR) {
				str = str.replace(newCmdRR.strRes, cmdRR.strRes);
			}
			
			template.set(lineNumber, str);
			
			// check if line is a command
			CommandResult res = parseCommand(template, lineNumber, templateFile,
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
					// check next line after what was added
					lineNumber += res.added.size() - 1;
				} else {
					// specified lines removed, nothing added, recheck current line
					lineNumber--;
				}
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
	 *      null if no command found in this line.
	 */
	private CommandResult parseCommand(
					ArrayList<String> template,
					int lineNumber,
					String templateFile,
					Map<String, Param> parameters,
					Map<String, String> variables
	) {
		String line = template.get(lineNumber);
		// try to find $command$ section in provided line
		if (null == line) {
			Utilities.log("Error: Line to check for command was null!");
			return null;
		}
		
		RegexResult cmdRR = Utilities.firstMatch(line, cmdPattern, 0);
		if (null == cmdRR) {
			// not a command -> no action required
			
			// check for single $, could have been an unclosed command
			Pattern singleDollar = Pattern.compile(commandIdentifier);
			RegexResult unclosedCommand = Utilities.firstMatch(line, singleDollar, 0);
			
			if (null != unclosedCommand) {
				Utilities.log("Warning: single $ found in output on line : " +
								(lineNumber+1) + " : " + line + " produced by " + templateFile +
								", " + "possible unclosed command : Line copied!");
			}
			
			return null;
		}
		
		String cmd = cmdRR.strRes;
		switch (cmd) {
			case "var" :
				return varCmd(line, lineNumber, templateFile, variables);
			case "vars" :
				return varsCmd(line, lineNumber, templateFile, variables);
			case "p-template" :
				return pTemplateCmd(line,lineNumber, templateFile, parameters, variables);
			case "p-line" :
				return pLineCmd(line, lineNumber, parameters);
			case "template" :
				return templateCmd(line, lineNumber, templateFile, parameters, variables);
			case "p-block" :
				return pBlockCmd(template, lineNumber, parameters, variables);
			default :
				Utilities.log("Error: Unrecognized command : " + cmd);
				return new CommandResult(1, null);
		}
	}
	
	/**
	 * Checks if a template line defines an additional variable.
	 *
	 * @param line
	 *      The line containing the command.
	 * @param lineNumber
	 *      The linenumber of the line containing the command, used for warnings.
	 * @param templateFile
	 *      Filename of template that is being checked.
	 * @param variables
	 *      Existing set of variables to add to.
	 * @return
	 *      Whether a variable definition was found.
	 */
	private CommandResult varCmd(
					String line,
					int lineNumber,
					String templateFile,
					Map<String, String> variables
	) {
		RegexResult varNameRR = Utilities.firstMatch(line, varNamePattern, 0);
		if (null == varNameRR) {
			Utilities.log("Warning: No variable name specified on line : " +
							lineNumber + " : " + line + " in " + templateFile + ": Line " +
							"ignored!");
			return new CommandResult(1, null);
		}
		String v_name = varNameRR.strRes;
		// remainder of string (one separating character) = variable value
		String v_value = line.substring((varNameRR.end+3));
		
		// if variable was already defined in parent template, warn user
		if (variables.containsValue(v_name)) {
			if (null == templateFile) {
				Utilities.log("Warning: template overrides parent template " +
								"variable " + v_name + " locally with value " + v_value);
			} else {
				Utilities.log("Warning: template " + templateFile +
								" overrides parent template variable " + v_name +
								" locally with value " + v_value);
			}
		}
		// store variable in map as key, value
		variables.put(v_name, v_value);
		// remove line afterwards
		return new CommandResult(1, null);
	}
	
	/**
	 * Allows variable definitions to be included from another file.
	 *
	 * @param line
	 *      The line containing the command.
	 * @param lineNumber
	 *      The linenumber of the line containing the command, used for warnings.
	 * @param templateFile
	 *      The template file in which the command line was found.
	 * @param variables
	 *      Existing set of variables to add to.
	 * @return
	 *      Whether a variable definition was found.
	 */
	private CommandResult varsCmd(
					String line,
					int lineNumber,
					String templateFile,
					Map<String, String> variables
	) {
		RegexResult fileName = Utilities.firstMatch(line, templateNamePattern, 0);
		if (null == fileName) {
			return noTemplateFile(line, lineNumber, templateFile);
		}
		
		ArrayList<String> lines = Utilities.readLinesFromFile(fileName.strRes);
		
		if (null == lines) {
			// file not found, user is already warned
			return new CommandResult(1, null);
		}
		
		int size = lines.size();
		for (int i = 0; i < size; i++) {
			String str = lines.get(i);
			RegexResult varNameRR = Utilities.firstMatch(str, varNamePattern, 0);
			if (null == varNameRR) {
				Utilities.log("Warning: No variable name specified on line : " +
								i + " : " + str + " in " + fileName.strRes + ": Line ignored!");
				continue;
			}
			String v_name = varNameRR.strRes;
			// remainder of string (one separating character) = variable value
			String v_value = str.substring((varNameRR.end+3));
			
			// if variable was already defined in parent template, warn user
			if (variables.containsValue(v_name)) {
				Utilities.log("Warning: Line : " + i + " : " + str + " in " +
								fileName.strRes + " overrides parent " + "template variable " +
								v_name + " locally with value " + v_value);
			}
			variables.put(v_name, v_value);
		}
		
		return new CommandResult(1, null);
	}
	
	/**
	 * Handles the event of a command that required a specified template file but
	 * did not include a specified template name. Warns the user and produces a
	 * safe result.
	 *
	 * @param line
	 *      The line containing the command.
	 * @param templateFile
	 *      The template file in which the command line was found.
	 * @return
	 *      The result of a command that requires a specified template but did
	 *      not specify a template filename.
	 */
	private CommandResult noTemplateFile(
					String line,
					int lineNumber,
					String templateFile
	) {
		Pattern templExtPattern = Pattern.compile(templateExtension);
		RegexResult templExt = Utilities.firstMatch(line, templExtPattern, 0);
		if (null == templExt) {
			Utilities.log("Warning: No template extension found on line : " +
							lineNumber + " : " + line + " in " + templateFile + ": Line " +
							"ignored!");
		} else {
			Utilities.log("Warning: No template file specified on line : " +
							lineNumber + " : " + line + " in " + templateFile + ": Line " +
							"ignored!");
		}
		return new CommandResult(1, null);
	}
	
	/**
	 * Tries to find a parameter list in the specified line. Parameter lists
	 * have the format [param_name_1|param_name_2|...] and are used for several
	 * of the parameter commands.
	 * @param line
	 *      The line to search for a parameter list.
	 * @param lineNumber
	 *      The linenumber of the specified line, useful for warnings.
	 * @return
	 *      List of (assumed to be) parameter name strings if the line
	 *      contained a list format. Returns null otherwise.
	 */
	private String[] findParamList(String line, int lineNumber) {
		//TODO: use regex
		RegexResult paramListRR = Utilities.firstMatch(line, paramListPattern, 0);
		if (null == paramListRR) {
			Utilities.log("Error: no parameter list provided for command " +
							"on line : " + lineNumber + " : " + line + " : Line ignored!");
			return null;
		}
		String paramList = paramListRR.strRes;
		
		return paramList.split(paramListDelimiter);
	}
	
	/**
	 * Handles a line containing a $p-block$ command.
	 * @param template
	 *      Template the command was found in as list of Strings.
	 * @param lineNumber
	 *      Line number the command was found in. Used for warnings
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
		int block_start;
		int start_bracket = line.indexOf("\\{");
		if (-1 == start_bracket) {
			Utilities.log("Error: no starting bracket '\\{' for p-block " +
							"command" +
							" " +
							" : " + line + " on line : " + lineNumber + " : Line ignored!");
			//TODO: assuming start bracket is on same line
			return new CommandResult(1, null);
		} else {
			// block start is at lineNumber
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
				parameters.forEach((name, par) -> ps.add(par));
				ArrayList<Param> sorted = Param.sortParams(ps);
				sorted.forEach(par -> {
					// make local copy so parent variables aren't modified
					HashMap<String, String> vars = new HashMap<>(variables);
					ArrayList<String> pCode = new ArrayList<>(code);
					// add param vars and process
					par.addParamVariables(vars);
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
			par.addParamVariables(vars);
			ArrayList<String> blockLines = processTemplate(null, pCode, parameters,
							vars);
			result.addAll(blockLines);
		}
		return new CommandResult((block_end + 1 - block_start), result);
	}
	
	/**
	 * Handles a line containing a $p-template$ command. First tries to find
	 * the required parameter list for which to process the template. Tries to
	 * find and process the specified template once for each entry in the
	 * parameter list.
	 * @param line
	 *      The line the command was found in.
	 * @param lineNumber
	 *      The number of the line the command was found in. Used for warnings.
	 * @param templateFile
	 *      Filename of template that is being checked.
	 * @param parameters
	 *      The parameters that were specified for the current template.
	 * @param variables
	 *      The variables that were specified for the current template.
	 * @return
	 *      Resulting code from attempting to process the template for each
	 *      parameter in the command's parameter list, along with an indication
	 *      that one line should be removed (the line containing the command).
	 */
	private CommandResult pTemplateCmd(
					String line,
					int lineNumber,
					String templateFile,
					Map<String, Param> parameters,
					Map<String, String> variables
	) {
		// find indentation used
		String indent = Utilities.findIndentation(line);
		
		RegexResult paramListRR = Utilities.firstMatch(line, paramListPattern, 0);
		if (null == paramListRR) {
			Utilities.log("Error: no parameter list provided for command " +
							"on line : " + lineNumber + " : " + line + " : Line ignored!");
			return new CommandResult(1, null);
		}
		String[] params = paramListRR.strRes.split(paramListDelimiter);
		
		RegexResult fileName = Utilities.firstMatch(line, templateNamePattern,
						paramListRR.end);
		if (null == fileName) {
			return noTemplateFile(line, lineNumber, templateFile);
		}
		
		String template = fileName.strRes;
		
		ArrayList<String> lines = new ArrayList<>();
		
		boolean foundAllKeyword = false;
		// if keyword all is used in list -> fill template for all params
		for (String p : params) {
			if (p.equals("all")) {
				foundAllKeyword = true;
				// process parameter in order of id
				ArrayList<Param> ps = new ArrayList<>();
				parameters.forEach((name, par) -> ps.add(par));
				ArrayList<Param> sorted = Param.sortParams(ps);
				sorted.forEach(par -> {
					// fill in parameters for template name if present
					String temp = par.fillInParam(template);
					// make local copy so parent variables aren't modified
					HashMap<String, String> vars = new HashMap<>(variables);
					// add param vars and process
					par.addParamVariables(vars);
					ArrayList<String> newLines = processTemplate(temp, parameters,
									vars);
					checkParamTemplateResult(lines, newLines, temp, par);
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
				// fill in parameters for template name if present
				String temp = par.fillInParam(template);
				// make local copy so parent variables aren't modified
				HashMap<String, String> vars = new HashMap<>(variables);
				// add param vars and process
				par.addParamVariables(vars);
				ArrayList<String> newLines = processTemplate(temp, parameters,
								vars);
				checkParamTemplateResult(lines, newLines, temp, par);
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
		// find indentation used
		String indent = Utilities.findIndentation(line);
		// find specified list of parameters
		String[] params = findParamList(line, lineNumber);
		// ignore if not found (user is warned)
		if (null == params) {
			return new CommandResult(1, null);
		}
		
		int p_list_end = line.indexOf(']');
		String paramLine = line.substring(p_list_end + 2);
		
		ArrayList<String> lines = new ArrayList<>();
		boolean foundAllKeyword = false;
		// if keyword all is used in list -> fill line for all params
		for (String p : params) {
			if (p.equals("all")) {
				foundAllKeyword = true;
				// process parameter in order of id
				ArrayList<Param> ps = new ArrayList<>();
				parameters.forEach((name, par) -> ps.add(par));
				ArrayList<Param> sorted = Param.sortParams(ps);
				sorted.forEach(par -> {
					// fill in the code line with values of par
					lines.add(par.fillInParam(paramLine));
				});
				break;
			}
		}
		if (!foundAllKeyword) {
			// only fill line for specified parameters
			for (String p : params) {
				// fill in the template
				Param par = parameters.get(p);
				if (null == par) {
					Utilities.log("Error: Unknown parameter : " + p + " : Parameter " + "skipped!");
					continue;
				}
				lines.add(par.fillInParam(paramLine));
			}
		}
		if (continue_indentation) {
			addPrefix(lines, indent);
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
	 *      Line containing a $template$ command.
	 * @param templateFile
	 *      Filename of template that is being checked.
	 * @param parameters
	 *      Parameters specified for template containing this template command.
	 * @param variables
	 *      Variables specified for template containing this template command.
	 * @return
	 *      Output code produced from the template as a List of String objects.
	 */
	private CommandResult templateCmd(
					String line,
					int lineNumber,
					String templateFile,
					Map<String, Param> parameters,
					Map<String, String> variables
	) {
		// find indentation used
		String indent = Utilities.findIndentation(line);
		
		RegexResult fileName = Utilities.firstMatch(line, templateNamePattern, 0);
		if (null == fileName) {
			return noTemplateFile(line, lineNumber, templateFile);
		}
		
		String template = fileName.strRes;
		
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
	 */
	private void checkParamTemplateResult(
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
			// check if start of line matches comment regex
			String str = itr.next();
			RegexResult commentRR = Utilities.firstMatch(str, templateCommentPattern, 0);
			if (null != commentRR) {
				// start of line matched comment regex -> remove line
				itr.remove();
			}
		}
	}
}
