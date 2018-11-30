package json;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import org.json.JSONObject;

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
		return new JSONObject(readStringFromFile(fileName));
	}
	
	
	private static String readStringFromFile(String fileName) {
		//using try with resources to ensure closing of resources.
		try (FileReader fileReader = new FileReader(fileName);
		     BufferedReader bufferedReader = new BufferedReader(fileReader)) {
			return bufferedReaderToString(bufferedReader);
		} catch (IOException e) {
			// File can't be found, or an error occurs while reading the file.
			System.err.println("Error reading JSON from file " + fileName + "!");
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
