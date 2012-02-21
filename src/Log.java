import net.rim.device.api.system.EventLogger;

/**
 * A Model class that represents the log data for a transport shown by the detailed view/screen 
 * @author Shadid Haque
 *
 */
public class Log {
	/** Name of the Transport*/
	private String transport;
	/** HTTP response code */
	private int responseCode;
	/** Log of the test */
	private String log;
	/** URL used to run this test */
	private String url;
	/** Test result */
	private boolean pass;
	/** Content received via HTTP */
	private String content;
	/** Length of the content received over JTTP */
	private long contentLength;
		
	/**
	 * Constructor. Initializes the member variables.
	 * @param transport	Name of the transport to which this refers to.
	 */
	public Log(String transport){
		this.transport = transport;
		responseCode = -1;
		log = "";
		url = "No url available";
		pass = false;
		content = "No contents available";
		contentLength = -1;
	}
	
	/** Gettters and Setters */
	public long getContentLength() {
		return contentLength;
	}
	public void setContentLength(long contentLength) {		
		this.contentLength = contentLength;
	}
	public String getTransport() {
		return transport;
	}
	public void setTransport(String transport) {		
		this.transport = transport;
	}
	public int getResponseCode() {
		return responseCode;
	}
	public void setResponseCode(int responseCode) {
		this.responseCode = responseCode;
	}
	public String getLog() {
		return log;
	}
	public void setLog(String log) {
		this.log = log;
	}
	public void addlog(String msg){
		log+="\n"+msg;
		logEventLog("["+transport+"]"+msg);
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public boolean isPass() {
		return pass;
	}
	public void setPass(boolean pass) {
		this.pass = pass;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	
	public String toString(){
		return "Transport: "+ getTransport() + "\n"
		+"Result: "+ (pass?"pass":"fail") + "\n"
		+"Response: "+ getResponseCode() + "\n"
		+"Length: "+ getContentLength() + "\n"
		+"URL: "+ getUrl() + "\n"
		+"Log: \n"+ getLog() + "\n\n";				
	}
	
	private void logEventLog(String msg){
		EventLogger.logEvent(0x4e697e68da459c1cL, msg.getBytes(), EventLogger.ALWAYS_LOG);
	}
	
	/** END OF Getters and Setters */
	
}
