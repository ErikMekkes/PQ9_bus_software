/**
 * Represents the result of looking for a regex in a string.
 */
public class RegexResult {
	public int start;
	public int end;
	public String strRes;
	
	public RegexResult(int start, int end, String strRes) {
		this.start = start;
		this.end = end;
		this.strRes = strRes;
	}
}
