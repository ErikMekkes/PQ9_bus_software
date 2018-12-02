package Utilities;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;
import parameter_ids.Param;

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
	 * Format : param_id_value, param_id_name, data_type, default_value
	 * @param fileName
	 *      The local CSV file to read into memory as parameters.
	 * @return
	 *      ArrayList of param objects representing the CSV file.
	 */
	public static ArrayList<Param> readParamCSV(String fileName) {
		ArrayList<Param> params = new ArrayList<>();
		if (null == fileName) {
			return params;
		}
		ArrayList<String> lines = readLinesFromFile(fileName);
		if (null == lines) {
			return params;
		}
		lines.forEach(str -> {
			String[] parts = str.split(",");
			Param p = new Param(parts[0], parts[1], parts[2], parts[3]);
			params.add(p);
		});
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
		// Make new empty list for result
		ArrayList<String> code = new ArrayList<>();
		// Find file in resources folder
		ClassLoader classloader = Thread.currentThread().getContextClassLoader();
		InputStream fileStream = classloader.getResourceAsStream(fileName);
		// Find the referenced location, try to open with BufferedReader
		// try with resource -> resource gets closed automatically
		try (InputStreamReader fileReader = new InputStreamReader(fileStream);
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
		// Find the referenced location, check if should be overridden
		Path filePath= Paths.get(fileName);
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
			}
		}
		// try to open with BufferedReader resource -> gets closed automatically
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
	
	/**
	 * Reads the specified file into program memory as a single String.
	 * @param fileName
	 *      The local file to read into memory.
	 * @return
	 *      A String representation of the file.
	 */
	private static String readStringFromFile(String fileName) {
		// Find file in resources folder
		ClassLoader classloader = Thread.currentThread().getContextClassLoader();
		InputStream fileStream = classloader.getResourceAsStream(fileName);
		//using try with resources to ensure closing of resources.
		try (InputStreamReader fileReader = new InputStreamReader(fileStream);
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
}
