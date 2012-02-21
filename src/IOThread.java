import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.microedition.io.SocketConnection;
import javax.microedition.lcdui.CustomItem;

import net.rim.device.api.servicebook.ServiceBook;
import net.rim.device.api.servicebook.ServiceRecord;
import net.rim.device.api.synchronization.ConverterUtilities;
import net.rim.device.api.system.CoverageInfo;
import net.rim.device.api.system.WLANInfo;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.util.DataBuffer;
import net.rim.device.api.system.RadioStatusListener;
import net.rim.device.api.system.RadioInfo;
import net.rim.device.api.system.GPRSInfo;
import net.rim.device.api.ui.MenuItem;
import net.rim.device.api.ui.component.EditField;
import net.rim.device.api.ui.container.PopupScreen;
import net.rim.device.api.ui.container.DialogFieldManager;
import net.rim.blackberry.api.invoke.*; 
import net.rim.blackberry.api.mail.*;


/**
 * A Thread that is responsible for communicating over http through different transports.
 * 
 * This application is targeted for JDE 4.5 or later. If you are using earlier versions of the JDE
 * to compile this application, please make the following changes as instructed.
 * 
 * Some of the APIs used in this class are not available in certain versions
 * of the BlackBerry Handheld Software or JDE. Here is a list of such APIs used in this class:
 * 
 * 1. Before BlackBerry JDE 4.3, RadioStatusListener had an additional method 
 * mobilityManagementEvent(int eventCode, int cause). So we must implement this method 
 * if we are using BlackBerry JDE 4.2 or earlier. 
 * 
 * 2. CoverageInfo.COVERAGE_DIRECT is not supported until BlackBerry JDE 4.5. Please use 
 * CoverageInfo.COVERAGE_CARRIER if you are using BlackBerry JDE 4.3 or earlier.
 * 
 * 3. WLANInfo class is not available until BlackBerry JDE 4.5. Please use the following logic for BlackBerry
 * JDE 4.3 or earlier.
 * if(CoverageInfo.isCoverageSufficient(CoverageInfo.COVERAGE_CARRIER, RadioInfo.WAF_WLAN, false)){
 *			coverageWiFi = true;
 *			wifiLog.addlog("Coverage Status: Online");
 * } 
 * 
 * 4. RadioInfo.NETWORK_SERVICE_GAN is not supported in BlackBerry JDE 4.2.1 or earlier. Please remove references
 * to this constant for those versions of the JDE.
 * 
 * 5. RadioInfo.NETWORK_SERVICE_EVDO_ONLY and RadioInfo.NETWORK_SERVICE_UMTS are not supported in BlackBerry 
 * JDE 4.2.1 or earlier. Please remove references to these constants for those versions of the JDE where it is not
 * supported.
 * 
 * 6. The CoverageInfo class is introduced in JDE 4.2.0. Please remove references to this class if you are using 
 * an older version of the JDE to compile this application. However, it is possible to do the following in JDE
 * older than 4.2.0:
 * (RadioInfo.getSignalLevel() != RadioInfo.LEVEL_NO_COVERAGE) can confirm that that the device has radio signal.
 * and
 * (RadioInfo.getNetworkService() & RadioInfo.NETWORK_SERVICE_DATA)>0 can confirm that the device has data
 * connectivity. 
 * 
 * @author Shadid Haque
 *
 */
public class IOThread extends Thread implements RadioStatusListener{
	/** The URL provided by the user in InputScreen*/
	private String baseURL;
	/** Number of times this should retry each transport until it finally gives up */
	private int retries;
	/** Screen to show network information, the progress of each communication */
	private ReportScreen rs;
	/** Stores transport ServiceBooks if found. Otherwise, null */
	private ServiceRecord srMDS, srBIS, srWAP, srWAP2, srWiFi, srUnite;
	/** Log instances for each transport via HTTP */
	private Log tcpHTTPLog, bisHTTPLog, mdsHTTPLog, wapHTTPLog, wap2HTTPLog, wifiHTTPLog, uniteHTTPLog;
	/** Log instances for each transport via Sockets */
	private Log tcpSocketLog, bisSocketLog, mdsSocketLog, wapSocketLog, wap2SocketLog, wifiSocketLog, uniteSocketLog;
	/** Reference to the InputScreen */
	private InputScreen inputs;	
	/** Flags indicating the coverage status of each transport */
	private boolean coverageTCP=false, coverageMDS=false, coverageBIS=false, coverageWAP=false, coverageWAP2=false, coverageWiFi=false, coverageUnite=false;
	/** 
	 * Applies to WAP1.0 connection only.
	 * This is a flag to indicate if WAP parameters should be parsed from the ServiceBook.
	 * If user provides any WAP parameter on the InputScreen, this flag is set to false.
	 * TODO Currently parsing from service book is not supported. 
	 */
	private boolean wapParametersUnavailable=true;
	/** 
	 * CONFIG_TYPE_ constants which are used to find appropriate service books.
	 * TODO Currently only Unite is detected this way. 
	 */ 
	private static final int CONFIG_TYPE_WAP  = 0;
    private static final int CONFIG_TYPE_BES  = 1;
    private static final int CONFIG_TYPE_WIFI = 3;
    private static final int CONFIG_TYPE_BIS  = 4;
    private static final int CONFIG_TYPE_WAP2 = 7;
    private static final String UNITE_NAME = "Unite";

    /**
     * Constructor. Initializes the Log instances for each transport, baseURL and retries.
     * Also creates and pushes ReportScreen to the Display stack.
     * @param inputs	Reference to InputScreen instance
     */
	public IOThread(InputScreen inputs) {		
		tcpHTTPLog = new Log("Direct TCP (HTTP)");
		bisHTTPLog = new Log("BIS-B (HTTP)");
		mdsHTTPLog = new Log("MDS (HTTP)");
		wapHTTPLog = new Log("WAP (HTTP)");
		wap2HTTPLog = new Log("WAP2 (HTTP)");
		wifiHTTPLog = new Log("WiFi (HTTP)");
		uniteHTTPLog = new Log("Unite (HTTP)");
		
		tcpSocketLog = new Log("Direct TCP (Socket)");
		bisSocketLog = new Log("BIS-B (Socket)");
		mdsSocketLog = new Log("MDS (Socket)");
		wapSocketLog = new Log("WAP (Socket)");
		wap2SocketLog = new Log("WAP2 (Socket)");
		wifiSocketLog = new Log("WiFi (Socket)");
		uniteSocketLog = new Log("Unite (Socket)");
		
		this.inputs = inputs;
		
		/** 
		 * Formats URL typed by user so that it can be used for both http and socket. 
		 * Removes "http://" or "socket://" if the user entered them by mistake.
		 */
		this.baseURL = inputs.getEfHost();
		if(baseURL.indexOf("http://")!=-1)
			baseURL = baseURL.substring(7);
		if(baseURL.indexOf("https://")!=-1)
			baseURL = baseURL.substring(8);
		if(baseURL.indexOf("socket://")!=-1)
			baseURL = baseURL.substring(9);
		if(inputs.getEfPort().length()>0){
			if(baseURL.indexOf("/")!=-1)
				baseURL = baseURL.substring(0, baseURL.indexOf("/"))+":"+inputs.getEfPort()+baseURL.substring(baseURL.indexOf("/"));
			else
				baseURL+=":"+inputs.getEfPort();
		} else{
			if(baseURL.indexOf("/")!=-1)
				baseURL = baseURL.substring(0, baseURL.indexOf("/"))+":80"+baseURL.substring(baseURL.indexOf("/"));
			else
				baseURL+=":80";
		}
		
		
		this.retries = inputs.getEfRetries();
		
		rs = new ReportScreen(this);
		UiApplication.getUiApplication().pushScreen(rs);
	}

	/**
	 * Determines availability of each transport by calling checkTransportAvailability.
	 * If a transport is available, tries 'retries' many times to connect to the URL through
	 * that transport.  
	 */
	public void run() {
		/** Determines transport availability. */
		initializeTransportAvailability();
		
		/** 
		 * Rest of this method simply tries to communicate over different transports
		 * that are available. For each transport it will retry 'retries' many times if
		 * an attempt fails.
		 */
		
		/** Direct TCP using HTTP */
		if(inputs.isTestHTTP() && inputs.isDoTCP() && coverageTCP){
			rs.displayProgress("Direct TCP (HTTP)");
			for(int i=0; i<retries; i++){
				if(tcpHTTPLog.isPass())
					break;			
				rs.setTrial(i, retries);
				try {sleep(2000);} catch (InterruptedException e) {}
				doHTTPDirectTCP();
			}				
			rs.displayResult(tcpHTTPLog);
		} else if(!coverageTCP){
			tcpHTTPLog.addlog("Skipped test: Direct TCP coverage is not available");
			if(inputs.isDoTCP() && inputs.isTestHTTP())  
				rs.displayResult(tcpHTTPLog);
		}	
		
		/** Direct TCP using Socket */
		if(inputs.isTestSocket() && inputs.isDoTCP() && coverageTCP){
			rs.displayProgress("Direct TCP (Socket)");
			for(int i=0; i<retries; i++){
				if(tcpSocketLog.isPass())
					break;			
				rs.setTrial(i, retries);
				try {sleep(2000);} catch (InterruptedException e) {}
				doSocketDirectTCP();
			}				
			rs.displayResult(tcpSocketLog);
		} else if(!coverageTCP){
			tcpSocketLog.addlog("Skipped test: Direct TCP coverage is not available");
			if(inputs.isDoTCP() && inputs.isTestSocket())  
				rs.displayResult(tcpSocketLog);
		}	
				
		/** MDS using HTTP */
		if (inputs.isTestHTTP() && srMDS != null && inputs.isDoMDS() && coverageMDS){			
			rs.displayProgress("MDS (HTTP)");
			for(int i=0; i<retries; i++){
				if(mdsHTTPLog.isPass())
					break;
				rs.setTrial(i, retries);
				try {sleep(2000);} catch (InterruptedException e) {}
				doHTTPMDS();
			}
			rs.displayResult(mdsHTTPLog);
		} else{		
			if(srMDS==null)
				mdsHTTPLog.addlog("Skipped test: No MDS service records found.");
			if(!coverageMDS)
				mdsHTTPLog.addlog("Skipped test: MDS coverage is not available");
			if(inputs.isDoMDS() && inputs.isTestHTTP())
				rs.displayResult(mdsHTTPLog);
		}
		
		/** MDS using Socket */
		if (inputs.isTestSocket() && srMDS != null && inputs.isDoMDS() && coverageMDS){			
			rs.displayProgress("MDS (Socket)");
			for(int i=0; i<retries; i++){
				if(mdsSocketLog.isPass())
					break;
				rs.setTrial(i, retries);
				try {sleep(2000);} catch (InterruptedException e) {}
				doSocketMDS();
			}
			rs.displayResult(mdsSocketLog);
		} else{		
			if(srMDS==null)
				mdsSocketLog.addlog("Skipped test: No MDS service records found.");
			if(!coverageMDS)
				mdsSocketLog.addlog("Skipped test: MDS coverage is not available");
			if(inputs.isDoMDS() && inputs.isTestSocket())
				rs.displayResult(mdsSocketLog);
		}
		
		/** Unite using HTTP */
		if (inputs.isTestHTTP() && srUnite != null && inputs.isDoUnite() && coverageUnite){			
			rs.displayProgress("Unite (HTTP)");
			for(int i=0; i<retries; i++){
				if(uniteHTTPLog.isPass())
					break;
				rs.setTrial(i, retries);
				try {sleep(2000);} catch (InterruptedException e) {}
				doHTTPUnite();
			}
			rs.displayResult(uniteHTTPLog);
		} else{		
			if(srUnite==null)
				uniteHTTPLog.addlog("Skipped test: No Unite service records found.");
			if(!coverageUnite)
				uniteHTTPLog.addlog("Skipped test: Unite coverage is not available");
			if(inputs.isDoUnite() && inputs.isTestHTTP())
				rs.displayResult(uniteHTTPLog);
		}
		
		/** Unite using Socket */
		if (inputs.isTestSocket() && srUnite != null && inputs.isDoUnite() && coverageUnite){			
			rs.displayProgress("Unite (Socket)");
			for(int i=0; i<retries; i++){
				if(uniteSocketLog.isPass())
					break;
				rs.setTrial(i, retries);
				try {sleep(2000);} catch (InterruptedException e) {}
				doSocketUnite();
			}
			rs.displayResult(uniteSocketLog);
		} else{		
			if(srUnite==null)
				uniteSocketLog.addlog("Skipped test: No Unite service records found.");
			if(!coverageUnite)
				uniteSocketLog.addlog("Skipped test: Unite coverage is not available");
			if(inputs.isDoUnite() && inputs.isTestSocket())
				rs.displayResult(uniteSocketLog);
		}
		
		/** BIS-B using HTTP */
		if (inputs.isTestHTTP() && srBIS != null && inputs.isDoBIS() && coverageBIS){
			rs.displayProgress("BIS-B (HTTP)");
			for(int i=0; i<retries; i++){
				if(bisHTTPLog.isPass())
					break;
				rs.setTrial(i, retries);
				try {sleep(2000);} catch (InterruptedException e) {}
				doHTTPBIS();
			}
			rs.displayResult(bisHTTPLog);
		} else{
			if(srBIS==null)
				bisHTTPLog.addlog("Skipped test: No BIS-B service records found.");
			if(!coverageBIS)
				bisHTTPLog.addlog("Skipped test: BIS-B coverage is not available");
			if(inputs.isDoBIS() && inputs.isTestHTTP())
				rs.displayResult(bisHTTPLog);
		}
		
		/** BIS-B using Socket */
		if (inputs.isTestSocket() && srBIS != null && inputs.isDoBIS() && coverageBIS){
			rs.displayProgress("BIS-B (Socket)");
			for(int i=0; i<retries; i++){
				if(bisSocketLog.isPass())
					break;
				rs.setTrial(i, retries);
				try {sleep(2000);} catch (InterruptedException e) {}
				doSocketBIS();
			}
			rs.displayResult(bisSocketLog);
		} else{
			if(srBIS==null)
				bisSocketLog.addlog("Skipped test: No BIS-B service records found.");
			if(!coverageBIS)
				bisSocketLog.addlog("Skipped test: BIS-B coverage is not available");
			if(inputs.isDoBIS() && inputs.isTestSocket())
				rs.displayResult(bisSocketLog);
		}
		
		/** WAP1.0 usign HTTP */
		getWAPURL();
		if (inputs.isTestHTTP() && srWAP!=null && !wapParametersUnavailable && inputs.isDoWAP() && coverageWAP){
			rs.displayProgress("WAP (HTTP)");
			for(int i=0; i<retries; i++){
				if(wapHTTPLog.isPass())
					break;
				rs.setTrial(i, retries);
				try {sleep(2000);} catch (InterruptedException e) {}
				doHTTPWAP();
			}
			rs.displayResult(wapHTTPLog);
		} else{
			if(srWAP==null)
				wapHTTPLog.addlog("Skipped test: No WAP service records found.");
			if(!coverageWAP)
				wapHTTPLog.addlog("Skipped test: WAP coverage is not available");
			if(wapParametersUnavailable)
				wapHTTPLog.addlog("Skipped test: Please provide WAP parameters");
			if(inputs.isDoWAP() && inputs.isTestHTTP())
				rs.displayResult(wapHTTPLog);			
		}
		
		/** WAP1.0 using Socket*/
		getWAPURL();
		if (inputs.isTestSocket() && srWAP!=null && !wapParametersUnavailable && inputs.isDoWAP() && coverageWAP){
			rs.displayProgress("WAP (Socket)");
			for(int i=0; i<retries; i++){
				if(wapSocketLog.isPass())
					break;
				rs.setTrial(i, retries);
				try {sleep(2000);} catch (InterruptedException e) {}
				doSocketWAP();
			}
			rs.displayResult(wapSocketLog);
		} else{
			if(srWAP==null)
				wapSocketLog.addlog("Skipped test: No WAP service records found.");
			if(!coverageWAP)
				wapSocketLog.addlog("Skipped test: WAP coverage is not available");
			if(wapParametersUnavailable)
				wapSocketLog.addlog("Skipped test: Please provide WAP parameters");
			if(inputs.isDoWAP() && inputs.isTestSocket())
				rs.displayResult(wapSocketLog);			
		}
		
		/** WAP2.0 using HTTP */
		if (inputs.isTestHTTP() && srWAP2 != null && inputs.isDoWAP2() && coverageWAP2){
			rs.displayProgress("WAP2 (HTTP)");
			for(int i=0; i<retries; i++){
				if(wap2HTTPLog.isPass())
					break;
				rs.setTrial(i, retries);
				try {sleep(2000);} catch (InterruptedException e) {}
				doHTTPWAP2();
			}
			rs.displayResult(wap2HTTPLog);
		} else{
			if(srWAP2==null)
				wap2HTTPLog.addlog("Skipped test: No WAP2 service records found.");
			if(!coverageWAP2)
				wap2HTTPLog.addlog("Skipped test: WAP2 coverage is not available");
			if(inputs.isDoWAP2() && inputs.isTestHTTP())
				rs.displayResult(wap2HTTPLog);
		}
		
		/** WAP2.0 using Socket */
		if (inputs.isTestSocket() && srWAP2 != null && inputs.isDoWAP2() && coverageWAP2){
			rs.displayProgress("WAP2 (Socket)");
			for(int i=0; i<retries; i++){
				if(wap2SocketLog.isPass())
					break;
				rs.setTrial(i, retries);
				try {sleep(2000);} catch (InterruptedException e) {}
				doSocketWAP2();
			}
			rs.displayResult(wap2SocketLog);
		} else{
			if(srWAP2==null)
				wap2SocketLog.addlog("Skipped test: No WAP2 service records found.");
			if(!coverageWAP2)
				wap2SocketLog.addlog("Skipped test: WAP2 coverage is not available");
			if(inputs.isDoWAP2() && inputs.isTestSocket())
				rs.displayResult(wap2SocketLog);
		}
		
		/** WiFi using HTTP*/
		if (inputs.isTestHTTP() && srWiFi != null && inputs.isDoWiFi() && coverageWiFi){
			rs.displayProgress("WiFi (HTTP)");
			for(int i=0; i<retries; i++){
				if(wifiHTTPLog.isPass())
					break;
				rs.setTrial(i, retries);
				try {sleep(2000);} catch (InterruptedException e) {}
				doHTTPWiFi();
			}
			rs.displayResult(wifiHTTPLog);
		} else{
			if(srWiFi==null)
				wifiHTTPLog.addlog("Skipped test: No WiFi service records found.");
			if(!coverageWiFi)
				wifiHTTPLog.addlog("Skipped test: WiFi coverage is not available");
			if(inputs.isDoWiFi() && inputs.isTestHTTP())
				rs.displayResult(wifiHTTPLog);
		}
		
		/** WiFi using Socket*/
		if (inputs.isTestSocket() && srWiFi != null && inputs.isDoWiFi() && coverageWiFi){
			rs.displayProgress("WiFi (Socket)");
			for(int i=0; i<retries; i++){
				if(wifiSocketLog.isPass())
					break;
				rs.setTrial(i, retries);
				try {sleep(2000);} catch (InterruptedException e) {}
				doSocketWiFi();
			}
			rs.displayResult(wifiSocketLog);
		} else{
			if(srWiFi==null)
				wifiSocketLog.addlog("Skipped test: No WiFi service records found.");
			if(!coverageWiFi)
				wifiSocketLog.addlog("Skipped test: WiFi coverage is not available");
			if(inputs.isDoWiFi() && inputs.isTestSocket())
				rs.displayResult(wifiSocketLog);
		}	
	}

	/**
	 * Implementation of how to communicate via Direct TCP using HTTP
	 */
	private void doHTTPDirectTCP() {			
		tcpHTTPLog.setTransport("Direct TCP (HTTP)");		
		String httpURL = "http://" + getTCPURL();
		tcpHTTPLog.setUrl(httpURL);		
		getViaHTTP(tcpHTTPLog, httpURL);		
	}
	
	/**
	 * Implementation of how to communicate via Direct TCP using Socket
	 */
	private void doSocketDirectTCP() {		
		tcpSocketLog.setTransport("Direct TCP (Socket)");
		
		String tcpURL = getTCPURL();
		String socketURL=""; 
		if(tcpURL.indexOf("/")!=-1)
			socketURL = "socket://" + tcpURL.substring(0,tcpURL.indexOf("/"))+tcpURL.substring(tcpURL.indexOf(";"));
		else
			socketURL = "socket://"+tcpURL;
		tcpSocketLog.setUrl(socketURL);		
		
		String relativePath = "";
		if(tcpURL.indexOf("/")!=-1)
			relativePath = tcpURL.substring(tcpURL.indexOf("/"), tcpURL.indexOf(";"));
		else 
			relativePath = "/";
		
		getViaSocket(tcpSocketLog, socketURL, relativePath);		
	}
	
	/** 
	 * Constructs a Direct TCP url from the baseURL provided by the user
	 * @return	A url with Direct TCP parameters
	 */
	private String getTCPURL() {
		String url = baseURL + ";deviceside=true";
		String apn=inputs.getEfTcpAPN();
		String username = inputs.getEfTcpAPNUser();
		String password = inputs.getEfTcpAPNPassword();
		if(apn.length()>0)
			url+=";apn="+apn;
		if(username.length()>0)
			url+=";TunnelAuthUsername="+username;
		if(password.length()>0)
			url+=";TunnelAuthPassword="+password;
		return url;
	}	


	/**
	 * Implementation of how to communicate via MDS using HTTP
	 */
	private void doHTTPMDS() {
		mdsHTTPLog.setTransport("MDS (HTTP)");		
		String httpURL = "http://" + getMDSURL();
		mdsHTTPLog.setUrl(httpURL);		
		getViaHTTP(mdsHTTPLog, httpURL);	
	}
	
	/**
	 * Implementation of how to communicate via MDS using Socket
	 */
	private void doSocketMDS() {		
		mdsSocketLog.setTransport("MDS (Socket)");
		
		String mdsURL = getMDSURL();
		String socketURL=""; 
		if(mdsURL.indexOf("/")!=-1)
			socketURL = "socket://" + mdsURL.substring(0,mdsURL.indexOf("/"))+mdsURL.substring(mdsURL.indexOf(";"));
		else
			socketURL = "socket://"+mdsURL;
		mdsSocketLog.setUrl(socketURL);		
		
		String relativePath = "";
		if(mdsURL.indexOf("/")!=-1)
			relativePath = mdsURL.substring(mdsURL.indexOf("/"), mdsURL.indexOf(";"));
		else 
			relativePath = "/";
		
		getViaSocket(mdsSocketLog, socketURL, relativePath);
		
	}
	
	/** 
	 * Constructs a MDS url from the baseURL provided by the user
	 * @return	A url with MDS parameters
	 */
	private String getMDSURL() {
		return baseURL + ";deviceside=false";
	}

	/**
	 * Implementation of how to communicate via Unite using HTTP
	 */
	private void doHTTPUnite() {
		uniteHTTPLog.setTransport("Unite (HTTP)");		
		String httpURL = "http://" + getUniteURL();
		uniteHTTPLog.setUrl(httpURL);
		getViaHTTP(uniteHTTPLog, httpURL);		
	}
	
	/**
	 * Implementation of how to communicate via Unite using Socket
	 */
	private void doSocketUnite() {		
		uniteSocketLog.setTransport("Direct TCP (Socket)");
		
		String uniteURL = getUniteURL();
		String socketURL=""; 
		if(uniteURL.indexOf("/")!=-1)
			socketURL = "socket://" + uniteURL.substring(0,uniteURL.indexOf("/"))+uniteURL.substring(uniteURL.indexOf(";"));
		else
			socketURL = "socket://"+uniteURL;
		uniteSocketLog.setUrl(socketURL);		
		
		String relativePath = "";
		if(uniteURL.indexOf("/")!=-1)
			relativePath = uniteURL.substring(uniteURL.indexOf("/"), uniteURL.indexOf(";"));
		else 
			relativePath = "/";
		
		getViaSocket(uniteSocketLog, socketURL, relativePath);		
	}
	
	
	
	/** 
	 * Constructs a Unite url from the baseURL provided by the user
	 * @return	A url with Unite parameters
	 */
	private String getUniteURL() {
		return baseURL+ ";deviceside=false" + ";ConnectionUID=" + srUnite.getUid();
	}

	/**
	 * Implementation of how to communicate via BIS-B using HTTP
	 */
	private void doHTTPBIS() {
		bisHTTPLog.setTransport("BIS-B (HTTP)");		
		String httpURL = "http://" + getBISURL();
		bisHTTPLog.setUrl(baseURL+";***");		
		getViaHTTP(bisHTTPLog, httpURL);	
	}
	
	/**
	 * Implementation of how to communicate via BIS-B using Socket
	 */
	private void doSocketBIS() {		
		bisSocketLog.setTransport("BIS-B (Socket)");
		
		String bisURL = getBISURL();
		String socketURL=""; 
		if(bisURL.indexOf("/")!=-1)
			socketURL = "socket://" + bisURL.substring(0,bisURL.indexOf("/"))+bisURL.substring(bisURL.indexOf(";"));
		else
			socketURL = "socket://"+bisURL;
		bisSocketLog.setUrl(baseURL+";***");		
		
		String relativePath = "";
		if(bisURL.indexOf("/")!=-1)
			relativePath = bisURL.substring(bisURL.indexOf("/"), bisURL.indexOf(";"));
		else 
			relativePath = "/";
		
		getViaSocket(bisSocketLog, socketURL, relativePath);
		
	}
	
	/** 
	 * Constructs a BIS-B url from the baseURL provided by the user
	 * @return	A url with BIS-B parameters
	 */
	private String getBISURL() {
		return baseURL + ";deviceside=false"; // Not implemented since this is only available to ISV partners of RIM 
	}

	/**
	 * Implementation of how to communicate via WAP 2.0 using HTTP 
	 */
	private void doHTTPWAP2() {
		wap2HTTPLog.setTransport("WAP2 (HTTP)");		
		String httpURL = "http://" + getWAP2URL();
		wap2HTTPLog.setUrl(httpURL);		
		getViaHTTP(wap2HTTPLog, httpURL);	
	}
	
	/**
	 * Implementation of how to communicate via WAP 2.0 using Socket
	 */
	private void doSocketWAP2() {		
		wap2SocketLog.setTransport("WAP2 (Socket)");
		
		String wap2URL = getWAP2URL();
		String socketURL=""; 
		if(wap2URL.indexOf("/")!=-1)
			socketURL = "socket://" + wap2URL.substring(0,wap2URL.indexOf("/"))+wap2URL.substring(wap2URL.indexOf(";"));
		else
			socketURL = "socket://"+wap2URL;
		wap2SocketLog.setUrl(socketURL);		
		
		String relativePath = "";
		if(wap2URL.indexOf("/")!=-1)
			relativePath = wap2URL.substring(wap2URL.indexOf("/"), wap2URL.indexOf(";"));
		else 
			relativePath = "/";
		
		getViaSocket(wap2SocketLog, socketURL, relativePath);		
	}
	
	
	
	/** 
	 * Constructs a WAP2.0 url from the baseURL provided by the user
	 * @return	A url with WAP2.0 parameters
	 */
	private String getWAP2URL() {
		return baseURL+ ";deviceside=true" + ";ConnectionUID=" + srWAP2.getUid();
	}

	/**
	 * Implementation of how to communicate via WiFi using HTTP
	 */
	private void doHTTPWiFi() {
		wifiHTTPLog.setTransport("WiFi (HTTP)");		
		String httpURL = "http://" + getWiFiURL();
		wifiHTTPLog.setUrl(httpURL);		
		getViaHTTP(wifiHTTPLog, httpURL);	
	}	
	
	/**
	 * Implementation of how to communicate via WiFi using Socket
	 */
	private void doSocketWiFi() {		
		wifiSocketLog.setTransport("WiFi (Socket)");
		
		String wifiURL = getWiFiURL();
		String socketURL=""; 
		if(wifiURL.indexOf("/")!=-1)
			socketURL = "socket://" + wifiURL.substring(0,wifiURL.indexOf("/"))+wifiURL.substring(wifiURL.indexOf(";"));
		else
			socketURL = "socket://"+wifiURL;
		wifiSocketLog.setUrl(socketURL);		
		
		String relativePath = "";
		if(wifiURL.indexOf("/")!=-1)
			relativePath = wifiURL.substring(wifiURL.indexOf("/"), wifiURL.indexOf(";"));
		else 
			relativePath = "/";
		
		getViaSocket(wifiSocketLog, socketURL, relativePath);		
	}

	/** 
	 * Constructs a WiFi url from the baseURL provided by the user
	 * @return	A url with WiFi parameters
	 */
	private String getWiFiURL() {
		return baseURL+";interface=wifi";
	}

	/**
	 * Implementation of how to communicate via WAP 1.0 using HTTP
	 */
	private void doHTTPWAP() {
		wapHTTPLog.setTransport("WAP (HTTP)");		
		String httpURL = "http://" + getWAPURL();
		wapHTTPLog.setUrl(httpURL);		
		getViaHTTP(wapHTTPLog, httpURL);	
	}
	
	/**
	 * Implementation of how to communicate via WAP using Socket
	 */
	private void doSocketWAP() {		
		wapSocketLog.setTransport("WAP (Socket)");
		
		String wapURL = getWAPURL();
		String socketURL=""; 
		if(wapURL.indexOf("/")!=-1)
			socketURL = "socket://" + wapURL.substring(0,wapURL.indexOf("/"))+wapURL.substring(wapURL.indexOf(";"));
		else
			socketURL = "socket://"+wapURL;
		wapSocketLog.setUrl(socketURL);		
		
		String relativePath = "";
		if(wapURL.indexOf("/")!=-1)
			relativePath = wapURL.substring(wapURL.indexOf("/"), wapURL.indexOf(";"));
		else 
			relativePath = "/";
		
		getViaSocket(wapSocketLog, socketURL, relativePath);		
	}
	
	/** 
	 * Constructs a WAP1.0 url from the baseURL provided by the user
	 * @return	A url with WAP1.0 parameters
	 */
	private String getWAPURL() {
		String url=baseURL+";deviceside=true"; 
		String gatewayIP=inputs.getEfWapGatewayIP();
		String gatewayAPN=inputs.getEfWapGatewayAPN();
		String gatewayPort = inputs.getEfWapGatewayPort();
		String sourceIP = inputs.getEfWapSourceIP();
		String sourcePort = inputs.getEfWapSourcePort();
		String username = inputs.getEfWapUser();
		String password = inputs.getEfWapPassword();		
		if(gatewayIP.length()>0){
			url = url+";WapGatewayIP="+gatewayIP;
			wapParametersUnavailable = false;
		}		
		if(gatewayAPN.length()>0){
			url = url+";WapGatewayAPN="+gatewayAPN;
			wapParametersUnavailable = false;
		}
		if(gatewayPort.length()>0){
			url = url+";WapGatewayPort="+gatewayPort;
			wapParametersUnavailable = false;
		}
		if(sourceIP.length()>0){
			url = url+";WapSourceIP="+sourceIP;
			wapParametersUnavailable = false;
		}
		if(sourcePort.length()>0){
			url = url+";WapSourcePort="+sourcePort;
			wapParametersUnavailable = false;
		}
		if(username.length()>0){
			url = url+";TunnelAuthUsername="+username;
			wapParametersUnavailable = false;
		}
		if(password.length()>0){
			url = url+";TunnelAuthPassword="+password;
			wapParametersUnavailable = false;
		}
		if(inputs.getCfWapEnableWTLS()){
			url = url+";WapEnableWTLS=true";
			wapParametersUnavailable = false;
		}
		if(wapParametersUnavailable && srWAP!=null)
			return url;/** Not implemented */
		else 
			return url;
	}


	private void getViaHTTP(Log log, String url){
		try {			
			HttpConnection hconn;	
			if(url.indexOf("ConnectionType=mds-public")!=-1)
				log.addlog("Connecting to "+baseURL+";***");
			else
				log.addlog("Connecting to "+url);
			log.addlog("Openning connection");
			hconn = (HttpConnection) Connector.open(url);
			log.addlog("Connection opened");
			
			log.addlog("Getting response code");
			int response = hconn.getResponseCode();		
			log.setResponseCode(response);
			log.addlog("Got response: "+response);
			
			log.addlog("Reading content");
			InputStream is = hconn.openInputStream();
			String result = read(is);
			log.setContent(result);
			log.setContentLength(result.length());
			log.addlog("Received content. Length: "+result.length());
			
			log.addlog("Closing connection");
			hconn.close();
			log.addlog("Connection closed");
			log.setPass(true);
		} catch (Throwable e) {
			log.addlog(e.getMessage());
		} finally {			
			log.addlog("========END OF LOG========");			
		}
	}
	
	private void getViaSocket(Log log, String url, String relativePath){		
		SocketConnection sconn;		
		try {
			if(url.indexOf("ConnectionType=mds-public")!=-1)
				log.addlog("Connecting socket to "+baseURL+";***Only disclosed to ISV partners of RIM.");
			else
				log.addlog("Connecting socket to "+url);
					
			log.addlog("Openning connection");
			sconn = (SocketConnection) Connector.open(url);
			log.addlog("Connection opened");
			
			OutputStream os = sconn.openOutputStream();
			log.addlog("OutputStream opened");
			
			String getCommand = "GET " + relativePath + " HTTP/1.0\r\n\r\n";						     
							      
			os.write(getCommand.getBytes());				
			os.flush();			
			log.addlog("Sent:\n" + getCommand);			
			
			InputStream is = sconn.openInputStream();
			String result = read(is);
			if(result.indexOf("HTTP/")!=-1){
				try{ log.setResponseCode(Integer.parseInt(result.substring(9, 12))); } catch(NumberFormatException nfe){}
			}
			log.setContent(result);
			log.setContentLength(result.length());
			log.addlog("Received content. Length: "+result.length());
			
			log.addlog("Closing connection");
			sconn.close();
			log.addlog("Connection closed");
			log.setPass(true);		
		} catch (Throwable e) {
			log.addlog(e.getMessage());
		} finally {			
			log.addlog("========END OF LOG========");			
		}
	}
	
	/**
	 * Reads the content of a given InputStream and returns the content as a String
	 * @param is	InputStream to read from
	 * @return	Content as String
	 * @throws Throwable	Will be caught by the caller
	 */
	private String read(InputStream is) throws Throwable{
		String result = "";
		byte[] buffer = new byte[250];
		long cursor = 0;
		int offset = 0;
		
		while ((offset = is.read(buffer)) != -1) {
			result += new String(buffer, 0, offset);
			cursor += offset;
		}
		is.close();
		
		return result;
	}

	
	/**
	 * Initializes the ServiceRecord instances for each transport (if available). Otherwise leaves it null.
	 * Also determines if sufficient coverage is available for each transport and sets coverage* flags.
	 */
	private void initializeTransportAvailability() {
		ServiceBook sb = ServiceBook.getSB();
		ServiceRecord[] records = sb.getRecords();

		for (int i = 0; i < records.length; i++) {
			ServiceRecord myRecord = records[i];
			String cid, uid;

			if (myRecord.isValid() && !myRecord.isDisabled()) {
				cid = myRecord.getCid().toLowerCase();
				uid = myRecord.getUid().toLowerCase();
				// BIS
				if (cid.indexOf("ippp") != -1 && uid.indexOf("gpmds") != -1) {
					srBIS = myRecord;
				}			
				
				// BES
				if (cid.indexOf("ippp") != -1 && uid.indexOf("gpmds") == -1) {
					srMDS = myRecord;
				}
				// WiFi
				if (cid.indexOf("wptcp") != -1 && uid.indexOf("wifi") != -1) {
					srWiFi = myRecord;
				}		
				// Wap1.0
				if (getConfigType(myRecord)==CONFIG_TYPE_WAP && cid.equalsIgnoreCase("wap")) {
					srWAP = myRecord;
				}
				// Wap2.0
				if (cid.indexOf("wptcp") != -1 && uid.indexOf("wifi") == -1 && uid.indexOf("mms") == -1) {
					srWAP2 = myRecord;
				}
				// Unite
				if(getConfigType(myRecord) == CONFIG_TYPE_BES && myRecord.getName().equals(UNITE_NAME)) {
					srUnite = myRecord;
				}
			}	
		}		
		if(CoverageInfo.isCoverageSufficient(CoverageInfo.COVERAGE_BIS_B)){
			coverageBIS=true;	
			bisHTTPLog.addlog("Coverage Status: Online");
		}		
		if(CoverageInfo.isCoverageSufficient(CoverageInfo.COVERAGE_DIRECT)){
			coverageTCP=true;
			tcpHTTPLog.addlog("Coverage Status: Online");
			coverageWAP=true;
			wapHTTPLog.addlog("Coverage Status: Online");
			coverageWAP2=true;
			wap2HTTPLog.addlog("Coverage Status: Online");
		}
		if(CoverageInfo.isCoverageSufficient(CoverageInfo.COVERAGE_MDS)){			
			coverageMDS=true;
			mdsHTTPLog.addlog("Coverage Status: Online");
			coverageUnite=true;
			uniteHTTPLog.addlog("Coverage Status: Online");
		}	
		
		if(WLANInfo.getWLANState()==WLANInfo.WLAN_STATE_CONNECTED){
			coverageWiFi = true;
			wifiHTTPLog.addlog("Coverage Status: Online");
		}	
		
			
	}	
	
    /**
     * Gets the config type of a ServiceRecord using getDataInt below
     * @param record	A ServiceRecord
     * @return	configType of the ServiceRecord
     */
    private int getConfigType(ServiceRecord record) {
        return getDataInt(record, 12);
    }

    /**
     * Gets the config type of a ServiceRecord. Passing 12 as type returns the configType.    
     * @param record	A ServiceRecord
     * @param type	dataType
     * @return	configType
     */
    private int getDataInt(ServiceRecord record, int type)
    {
        DataBuffer buffer = null;
        buffer = getDataBuffer(record, type);
        
        if (buffer != null){
            try {
                return ConverterUtilities.readInt(buffer);
            } catch (EOFException e) {
                        return -1;
            }
        }
        return -1;
    }

    /** 
     * Utility Method for getDataInt()
     */
    private DataBuffer getDataBuffer(ServiceRecord record, int type) {
        byte[] data = record.getApplicationData();
            if (data != null) {
                DataBuffer buffer = new DataBuffer(data, 0, data.length, true);
                try {
                        buffer.readByte();
                    } catch (EOFException e1) {
                        return null;
                }
                if (ConverterUtilities.findType(buffer, type)) {
                    return buffer;
                }
            }
            return null;
    }

    
    /************ RadioStatusListener Implementation**************/
	public void baseStationChange() {
		addToAllLogs("RadioInfo: Base station change");		
	}

	public void networkScanComplete(boolean success) {
		addToAllLogs("RadioInfo: Network scan completed");	
	}

	public void networkServiceChange(int networkId, int service) {
		String services = "";
		if((service & RadioInfo.NETWORK_SERVICE_DATA)>0)
			services+="Data + ";
		if((service & RadioInfo.NETWORK_SERVICE_DIRECT_CONNECT)>0)
			services+="Direct Connect + ";
		if((service & RadioInfo.NETWORK_SERVICE_E911_CALLBACK_MODE)>0)
			services+="E911 Callback + ";
		if((service & RadioInfo.NETWORK_SERVICE_EDGE)>0)
			services+="EDGE + ";
		if((service & RadioInfo.NETWORK_SERVICE_EMERGENCY_ONLY)>0)
			services+="Emergency Only + ";
		if((service & RadioInfo.NETWORK_SERVICE_EVDO)>0)
			services+="EVDO + ";
		if((service & RadioInfo.NETWORK_SERVICE_EVDO_ONLY)>0)
			services+="EVDO Only + ";
		if((service & RadioInfo.NETWORK_SERVICE_GAN)>0)
			services+="GAN + ";
		if((service & RadioInfo.NETWORK_SERVICE_IN_CITY_ZONE)>0)
			services+="City Zone + ";
		if((service & RadioInfo.NETWORK_SERVICE_IN_HOME_ZONE)>0)
			services+="Home Zone + ";
		if((service & RadioInfo.NETWORK_SERVICE_MODEM_MODE_ENABLED)>0)
			services+="Modem Mode + ";
		if((service & RadioInfo.NETWORK_SERVICE_ROAMING)>0)
			services+="Roaming + ";
		if((service & RadioInfo.NETWORK_SERVICE_ROAMING_OFF_CAMPUS)>0)
			services+="Roaming Off-campus + ";
		if((service & RadioInfo.NETWORK_SERVICE_SUPPRESS_ROAMING)>0)
			services+="Supress Roaming + ";
		if((service & RadioInfo.NETWORK_SERVICE_UMTS)>0)
			services+="UMTS + ";
		if((service & RadioInfo.NETWORK_SERVICE_VOICE)>0)
			services+="Voice + ";
		if(services.length()>2)
			addToAllLogs("Network Services Changed: "+services.substring(0,services.length()-2));
		else
			addToAllLogs("Network Services Changed: "+networkId+" Services: "+"No Service Found");
	}

	public void networkStarted(int networkId, int service) {
		String services = "";
		if((service & RadioInfo.NETWORK_SERVICE_DATA)>0)
			services+="Data + ";
		if((service & RadioInfo.NETWORK_SERVICE_DIRECT_CONNECT)>0)
			services+="Direct Connect + ";
		if((service & RadioInfo.NETWORK_SERVICE_E911_CALLBACK_MODE)>0)
			services+="E911 Callback + ";
		if((service & RadioInfo.NETWORK_SERVICE_EDGE)>0)
			services+="EDGE + ";
		if((service & RadioInfo.NETWORK_SERVICE_EMERGENCY_ONLY)>0)
			services+="Emergency Only + ";
		if((service & RadioInfo.NETWORK_SERVICE_EVDO)>0)
			services+="EVDO + ";
		if((service & RadioInfo.NETWORK_SERVICE_EVDO_ONLY)>0)
			services+="EVDO Only + ";
		if((service & RadioInfo.NETWORK_SERVICE_GAN)>0)
			services+="GAN + ";
		if((service & RadioInfo.NETWORK_SERVICE_IN_CITY_ZONE)>0)
			services+="City Zone + ";
		if((service & RadioInfo.NETWORK_SERVICE_IN_HOME_ZONE)>0)
			services+="Home Zone + ";
		if((service & RadioInfo.NETWORK_SERVICE_MODEM_MODE_ENABLED)>0)
			services+="Modem Mode + ";
		if((service & RadioInfo.NETWORK_SERVICE_ROAMING)>0)
			services+="Roaming + ";
		if((service & RadioInfo.NETWORK_SERVICE_ROAMING_OFF_CAMPUS)>0)
			services+="Roaming Off-campus + ";
		if((service & RadioInfo.NETWORK_SERVICE_SUPPRESS_ROAMING)>0)
			services+="Supress Roaming + ";
		if((service & RadioInfo.NETWORK_SERVICE_UMTS)>0)
			services+="UMTS + ";
		if((service & RadioInfo.NETWORK_SERVICE_VOICE)>0)
			services+="Voice + ";
		if(services.length()>2)
			addToAllLogs("Network Started: "+networkId+" Services: "+services.substring(0,services.length()-2));
		else
			addToAllLogs("Network Started: "+networkId+" Services: "+"No Service Found");
	}

	public void networkStateChange(int state) {
		if((state & GPRSInfo.GPRS_STATE_IDLE)>0)
			addToAllLogs("Network State Changed: GPRS STATE IDLE");
		else if((state & GPRSInfo.GPRS_STATE_READY)>0)
			addToAllLogs("Network State Changed: GPRS STATE READY");
		else if((state & GPRSInfo.GPRS_STATE_STANDBY)>0)
			addToAllLogs("Network State Changed: GPRS STATE STANDBY");
	}

	public void pdpStateChange(int apn, int state, int cause) {
		// No idea what this is.		
	}

	public void radioTurnedOff() {
		addToAllLogs("Radio Turned On");		
	}

	public void signalLevel(int level) {
		if(level==RadioInfo.LEVEL_NO_COVERAGE)
			addToAllLogs("Signal Level Changed: No Coverage");
		else
			addToAllLogs("Signal Level Changed: "+level+" dBm");		
	}
	/********* END OF RadioStatusListener **********/
	
	/**
	 * Adds a String to all Log instances.
	 * @param msg	Message to add to all Log instances.
	 */
	public void addToAllLogs(String msg){
		tcpHTTPLog.addlog(msg);
		bisHTTPLog.addlog(msg);
		mdsHTTPLog.addlog(msg);
		wapHTTPLog.addlog(msg);
		wap2HTTPLog.addlog(msg);
		wifiHTTPLog.addlog(msg);
		uniteHTTPLog.addlog(msg);
	}	
}
