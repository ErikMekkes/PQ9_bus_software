import java.util.ArrayList;

/**
 * This class represents the result of a command as a number of lines to remove
 * from the template and a set of lines to add.
 */
public class CommandResult {
	public int removed;
	public ArrayList<String> added;
	
	/**
	 * Represents the result of a command as a number of lines to remove from
	 * the template and a set of lines to add.
	 *
	 * @param removed
	 *      Lines to remove from the template.
	 * @param added
	 *      Lines to add to the template.
	 */
	public CommandResult(int removed, ArrayList<String> added) {
		this.removed = removed;
		this.added = added;
	}
}
