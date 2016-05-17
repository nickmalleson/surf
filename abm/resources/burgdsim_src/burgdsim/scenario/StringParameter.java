package burgdsim.scenario;

/**
 * Class used to represent String parameters. Can only be static (not possible to increment a String).
 * @author nick
 *
 */
public class StringParameter extends Parameter<String> {

	private static final long serialVersionUID = 1L;

	public StringParameter(String name, String value) {
		super(name, value);
	}

}
