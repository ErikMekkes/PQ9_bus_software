package json;

import java.io.*;
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;
import parameter_ids.Param;

/**
 * JSON utilities for codeGeneration.
 * @author Erik
 */
public class json {
	
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
			e.printStackTrace();
			System.out.println("Error reading JSON from file " + fileName + "!");
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
		ArrayList<String> lines = readFromFile(fileName);
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
	 * Loads the specified file into program memory per line as a list of
	 * Strings.
	 *
	 * @param fileName
	 *          The local file to read into memory.
	 * @return
	 *          An ArrayList of Strings, each element is a line from the file.
	 */
	public static ArrayList<String> readFromFile(String fileName) {
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
