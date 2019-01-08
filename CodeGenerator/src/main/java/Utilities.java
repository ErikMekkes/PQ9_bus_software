import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Utilities for codeGeneration.
 * @author Erik
 */
public class Utilities {
	
	/**
	 * Attempts to read a specified file as a JSONObject
	 * @param fileName
	 *      JSON file to read.
	 * @return
	 *      JSON Object read from file.
	 */
	public static JSONObject readJSONFromFile(String fileName) {
		try {
			String str = readStringFromFile(fileName);
			if (null == str) {
				return null;
			}
			return new JSONObject(str);
		} catch (JSONException e) {
			System.out.println("Error reading JSON from file " + fileName + "!");
			System.out.println(e.getMessage());
			return null;
		}
	}
	
	/**
	 * Reads a CSV file containing parameters as lines of comma separated values.
	 * Format : param_id_value, param_id_name, data_type, default_value.
	 *
	 * Allows an integer start value to be specified for auto incrementing
	 * parameter ids. The parameters from the csv file will be given ids
	 * in order of appearance starting from this value instead of whatever ids
	 * may have been specified in the csv file. Specify value as -1 to disable.
	 *
	 * Warns when ids were overridden by auto-incrementing, either disable
	 * auto-incrementing or explicitly make ids in csv 'unspecified' by
	 * setting their id to -1 to prevent this warning.
	 *
	 * @param fileName
	 *      The local CSV file to read into memory as parameters.
	 * @param auto_increment_start_id
	 *      The start value to be used for auto incrementing ids. -1 to disable.
	 * @return
	 *      ArrayList of param objects representing the CSV file.
	 */
	public static ArrayList<Param> readParamCSV(String fileName, int auto_increment_start_id) {
		
		if (null == fileName) {
			return null;
		}
		ArrayList<String> lines = readLinesFromFile(fileName);
		if (null == lines) {
			return null;
		}
		
		ArrayList<Param> params = new ArrayList<>();
		// start counter and keep track if we found a specified id
		int current_id = auto_increment_start_id;
		boolean found_specified_id = false;
		
		for (String str : lines) {
			String[] parts = str.split(",", -1);
			Param p;
			
			if (-1 != auto_increment_start_id) {
				// should use auto-incrementing ids
				String id_str = parts[0];
				// read specified id (string -> int)
				try {
					if (null == id_str) {
						throw new NumberFormatException();
					} else {
						int id = Integer.parseInt(id_str);
						// if id is not -1 ít was specified
						if (-1 != id) {
							found_specified_id = true;
						}
					}
				} catch (NumberFormatException e) {
					System.err.println("id value for parameter is not a number : " +
									id_str + "," + parts[1] + "," + parts[2] + "," + parts[3]);
				}
				
				p = new Param(current_id, parts[1], parts[2], parts[3]);
				current_id++;
			} else {
				// use specified ids
				p = new Param(parts[0], parts[1], parts[2], parts[3]);
			}
			params.add(p);
		}
		
		// incrementing id was used, but some ids were specified (not -1)
		if (found_specified_id) {
			System.out.print("Warning: auto increment enabled for parameter ids," +
							" id values from " + fileName + " ignored!");
			System.out.println(" Set id values to -1 in main .csv file to prevent " +
							"this warning.");
		}
		
		return params;
	}
	
	
	/**
	 * Reads the specified file into program memory per line as a list of
	 * Strings.
	 *
	 * @param fileName
	 *          The local file to read into memory.
	 * @return
	 *          An ArrayList of Strings, each element is a line from the file.
	 */
	public static ArrayList<String> readLinesFromFile(String fileName) {
		if (null == fileName) {
			System.err.println("Error: File name for readLinesFromFile function was" +
							" null!");
			return null;
		}
		// Make new empty list for result
		ArrayList<String> code = new ArrayList<>();
		// Find the referenced location, try to open with BufferedReader
		// try with resource -> resource gets closed automatically
		try (FileReader fileReader = new FileReader(fileName);
		     BufferedReader bufferedReader = new BufferedReader(fileReader)){
			// loop through each line in BufferedReader, add to result list
			bufferedReader.lines().forEach(code::add);
			return code;
		} catch (FileNotFoundException e) {
			System.err.println("File " + fileName + " not found!");
			return null;
		} catch (IOException e) {
			System.err.println("Error reading from file " + fileName + "!");
			return null;
		}
	}
	
	/**
	 * Writes the specified list of Strings to a file as separate lines.
	 * @param code
	 *      ArrayList of Strings to write to file.
	 * @param fileName
	 *      File path to write to.
	 * @param overwriteExisting
	 *      Whether or not to overwrite file if file already exists.
	 */
	public static void writeLinesToFile(ArrayList<String> code,
	                                     String fileName,
	                                     boolean overwriteExisting) {
		//TODO : make this return a boolean as indication for success
		// Find the referenced location, check if should be overridden
		Path filePath = Paths.get(fileName);
		if (Files.exists(filePath)) {
			if (overwriteExisting) {
				try {
					Files.delete(filePath);
				} catch (IOException e) {
					System.err.println(
									"Error: Unable to overwrite file : " + fileName + "!");
				}
			} else {
				System.err.println("Warning: file " + fileName + " already exists and" +
								" overwriting is disabled. This file was skipped!");
				return;
			}
		}
		
		// check if directories for file exist, if not create them
		Path parentDir = filePath.getParent();
		if (!Files.exists(parentDir)) {
			try {
				Files.createDirectories(parentDir);
			} catch (IOException e) {
				System.err.println("Error: Unable to create parent directories for " +
								fileName);
			}
		}
		
		// try to open with BufferedReader resource -> gets closed automatically
		try (BufferedWriter br = Files.newBufferedWriter(filePath,
						Charset.forName("UTF-8"))){
			if (null == code) {
				return;
			}
			// Loop through each list item, write to the file with a linebreak
			code.forEach((str) -> {
				if (null == str) return;
				try {
					br.write(str);
					br.newLine();
				} catch (IOException e) {
					System.err.println("Error: writing string" + str + "to " +
									"file!");
				}
			});
		} catch (FileNotFoundException e) {
			System.err.println("File not found!");
		} catch (IOException e) {
			System.err.println("Error writing to file!");
		}
	}
	
	/**
	 * Reads the specified file into program memory as a single String.
	 * @param fileName
	 *      The local file to read into memory.
	 * @return
	 *      A String representation of the file.
	 */
	private static String readStringFromFile(String fileName) {
		//using try with resources to ensure closing of resources.
		try (FileReader fileReader = new FileReader(fileName);
		     BufferedReader bufferedReader = new BufferedReader(fileReader)) {
			return bufferedReaderToString(bufferedReader);
		} catch (IOException e) {
			// File can't be found, or an error occurs while reading the file.
			System.err.println("Error reading from file " + fileName + "!");
			return null;
		}
	}
	
	/**
	 * Converts the contents of a BufferedReader to a String by reading each
	 * Character from the BufferedReader and adding it to a String using a
	 * StringBuilder.
	 *
	 * @param bufferedReader
	 * 			Reader from which the contents should be parsed into a String.
	 * @return
	 * 			Returns the contents of the BufferedReader as a String.
	 * @throws IOException
	 * 			Throws an IOException if the specified BufferedReader can not
	 * 			be read.
	 */
	private static String bufferedReaderToString(BufferedReader bufferedReader)
					throws IOException {
		StringBuilder sb = new StringBuilder();
		int cp;
		while ((cp = bufferedReader.read()) != -1) {
			sb.append((char) cp);
		}
		return sb.toString();
	}
	
	public static void checkBraces(
					ArrayList<String> code,
					String fileName,
					String templateFile
	) {
		int braces = 0;
		int size = code.size();
		int i;
		for (i = 0; i < size; i++) {
			String line = code.get(i);
			int lineBraces = countBraces(line);
			braces += lineBraces;
			if (braces < 0) {
				System.err.println("Error: Extra closing bracket on line " + i + " of" +
								" code produced by " + templateFile + " for " + fileName + "!");
				return;
			}
		}
		if (braces > 0) {
			System.err.println("Error: Missing closing bracket on line " + i + " of" +
							" code produced by " + templateFile + " for " + fileName + "!");
		}
	}
	
	private static int countBraces(String line) {
		int braces = 0;
		char[] chars = line.toCharArray();
		int size = chars.length;
		for (int i = 0; i < size; i++) {
			if (chars[i] == '{') {
				braces++;
			}
			if (chars[i] == '}') {
				braces--;
			}
		}
		return braces;
	}
}
