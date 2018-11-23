package parameter_ids;

public class LineBuilder {
	private String baseIndent;
	private StringBuilder sb;
	
	public LineBuilder() {
		this.baseIndent = "";
		this.sb = new StringBuilder();
	}
	
	public LineBuilder(String baseIndent) {
		this.baseIndent = baseIndent;
		this.sb = new StringBuilder();
	}
	
	/**
	 * Adds specified String as a new line with indent at the start and newline
	 * at end.
	 *
	 * @param line String to add to the current code section.
	 */
	public void add(String line){
		sb.append(baseIndent);
		sb.append(line);
		sb.append("\n");
	}
	
	public void addIndentOnly(String string) {
		sb.append(baseIndent);
		sb.append(string);
	}
	
	public String toString() {
		return sb.toString();
	}
	
	public void setIndent(String indent) {
		this.baseIndent = indent;
	}
}
