import net.rim.device.api.system.EventLogger;
import net.rim.device.api.ui.UiApplication;

/**
 * Application class. Contains the Entry point.
 * @author Shadid Haque  
 *
 */
public class NetworkDiagnostic extends UiApplication {
	private InputScreen ms;

	/**
	 * Constructor. Creates an InputScreen and pushes it on the display stack.
	 */
	public NetworkDiagnostic() {
		ms = new InputScreen();
		pushScreen(ms);
	}

	
	public static void main(String[] args) {
		NetworkDiagnostic app = new NetworkDiagnostic();
		EventLogger.register(0x4e697e68da459c1cL, "Network Diagnostic", EventLogger.VIEWER_STRING);
		app.enterEventDispatcher();
	}

	
}
