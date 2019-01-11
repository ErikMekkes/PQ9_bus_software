import java.io.*;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.rmi.CORBA.Util;

/**
 * Main program, starting point for executable jar file.
 */
public class Main {
	// Program defaults
	private static final String TEMPLATE_EXTENSION = ".cgen_template";
	private static final String TEMPLATE_DIR = "templates/";
	private static final String SETTINGS_FILE = "settings.json";
	// main parameter file location
	private static String parameters_file = "params.csv";
	// whether or not to overwrite existing files
	private static boolean overwrite_existing = true;
	private static boolean clear_directories = false;
	// whether or not to keep indentation for sub-templates
	private static boolean continue_indentation = true;
	// logging
	private static boolean logging = true;
	// start point for autoincrement parameter ids;
	private static int auto_increment_start_id = -1;
	
	// settings json file
	private static JSONObject settings;
	// main subsystem / directory name
	private static String subSysName = "generated_subsystem";
	// subdirectories
	private static ArrayList<String> subDirs = new ArrayList<>();
	// file for logging
	private static String logfile = "CodeGenerator.log";
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
		Utilities.log(intro);
		
		// load program settings
		settings = Utilities.readJSONFromFile(SETTINGS_FILE);
		if (null == settings) {
			return;
		} else {
			loadSettings();
		}
		
		// Create map of all available parameters
		params_list = Utilities.readParamCSV(parameters_file, auto_increment_start_id);
		params_list.forEach(par -> params.put(par.name, par));
		
		// generate all required directories specified in settings
		Utilities.log("\nMaking required directories...");
		makeDirs();
		
		// load subsystem parameters.
		Utilities.log("\nLoading subsystem parameters...");
		loadParams();
		
		// generate all required files specified in settings
		Utilities.log("Generating specified files...");
		makeFiles();
		
		// Indicate ending for user.
		String exit =
					"\nSuccessfully finished generating " + subSysName + " Subsystem!\n" +
					"Please do check the output files in ./" + subSysName +
					" and take any possible warnings or errors above into account";
		Utilities.log(exit);
		
		Utilities.log("A log file of the printed output above has been " +
						"created as " + logfile);
		
		if (logging) {
			Utilities.printLog(logfile, overwrite_existing);
		}
	}
	
	/**
	 * Loads various settings from the loaded json settings file.
	 */
	private static void loadSettings() {
		// find main directory from settings
		subSysName = settings.getString("subsystem_name");
		
		// find subdirectories from settings
		if (settings.has("subdirectories")) {
			if (settings.get("subdirectories") instanceof JSONArray) {
				JSONArray subdirectories = settings.getJSONArray("subdirectories");
				for (Object folderName : subdirectories) {
					if (folderName instanceof String) {
						subDirs.add((String) folderName);
					}
				}
			}
		}
		
		// find files to generate from settings
		// files in settings.json should be an object array = [{..},{..},...]
		if (settings.has("files_to_generate")) {
			JSONArray sFiles = settings.getJSONArray("files_to_generate");
			for (Object file : sFiles) {
				if (file instanceof JSONObject) {
					JSONObject fileObj = (JSONObject) file;
					// if a base template was specified, use that template
					if (fileObj.has("base_template")) {
						files.put(fileObj.getString("filename"), fileObj.getString("base_template"));
					} else {
						// use the filename to search for a base template
						files.put(fileObj.getString("filename"), fileObj.getString("filename") + TEMPLATE_EXTENSION);
					}
				}
			}
		}
		
		// overwriting existing files
		if (settings.has("overwrite_existing_files")) {
			if (settings.get("overwrite_existing_files") instanceof Boolean) {
				overwrite_existing = settings.getBoolean("overwrite_existing_files");
			}
		}
		
		// clear folders before generation
		if (settings.has("clear_existing_directories")) {
			if (settings.get("clear_existing_directories") instanceof Boolean) {
				clear_directories = settings.getBoolean("clear_existing_directories");
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
		
		// logging enabled?
		if (settings.has("logging")) {
			if (settings.get("logging") instanceof Boolean) {
				logging = settings.getBoolean("logging");
			}
		}
		
		// logfile specified?
		if (settings.has("logfile")) {
			if (settings.get("logfile") instanceof String) {
				logfile = (String) settings.get("logfile");
			}
		}
	}
	
	/**
	 * Loads specified parameters per file from settings.json.
	 */
	private static void loadParams() {
		if (!settings.has("files_to_generate")) {
			return;
		}
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
	 * Deletes pre-existing directories if clear folders is enabled.
	 */
	private static void makeDirs() {
		Path SubsFolder = Paths.get("./" + subSysName);
		
		if (Files.exists(SubsFolder) && clear_directories) {
			deleteDirectoryStream(SubsFolder);
		}
		
		// Try to make the main directory
		try {
			Files.createDirectory(SubsFolder);
		} catch(FileAlreadyExistsException e) {
			Utilities.log("Warning: The main directory " + subSysName + " " +
							"already" +
							" exists!");
		} catch (IOException e) {
			Utilities.log("Error: unable to create main directory " + subSysName + "!");
		}
		// Try to make the subdirectories
		subDirs.forEach(dir -> {
			Path subDir = Paths.get("./" + subSysName + "/" + dir);
			if (Files.exists(subDir) && clear_directories) {
				deleteDirectoryStream(subDir);
			}
			try {
				Files.createDirectory(subDir);
			} catch(FileAlreadyExistsException e) {
				Utilities.log("Warning: The subdirectory " + dir + " already " +
								"exists!");
			} catch (IOException e) {
				Utilities.log("Error: unable to create subdirectory " + dir + "!");
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
		TemplateProcessor tProc = new TemplateProcessor(subSysName, TEMPLATE_DIR,
						TEMPLATE_EXTENSION, continue_indentation, overwrite_existing);
		files.forEach((fileName,baseTemplate) -> {
			Utilities.log("\nProcessing file : " + fileName);
			Map<String, Param> pars = fileParams.get(fileName);
			ArrayList<String> codeLines =
							tProc.processTemplate(baseTemplate, pars);
			Utilities.checkBraces(codeLines, fileName, baseTemplate);
			Utilities.checkOpeningBraces(codeLines, fileName, baseTemplate);
			Utilities.writeLinesToFile(
							codeLines,
							subSysName + "/" + fileName, overwrite_existing
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
			Utilities.log("Error deleting " + subSysName + " folder!");
		}
	}
}
