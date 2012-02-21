import net.rim.device.api.ui.DrawStyle;
import net.rim.device.api.ui.component.ButtonField;
import net.rim.device.api.ui.component.CheckboxField;
import net.rim.device.api.ui.component.EditField;
import net.rim.device.api.ui.component.LabelField;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.SeparatorField;
import net.rim.device.api.ui.container.HorizontalFieldManager;
import net.rim.device.api.ui.container.MainScreen;
import net.rim.device.api.system.RadioInfo;
import net.rim.device.api.system.GPRSInfo;
import net.rim.device.api.system.CDMAInfo;
import java.io.*;
import javax.microedition.io.file.FileConnection;
import javax.microedition.io.Connector;

/**
 * This is the Screen that is displayed to the user when the application
 * launches. The user can input the url to connecct to, TCP APN parameters and
 * WAP1.0 parameters. Users can also select the transports they want to test.
 * 
 * @author Shadid Haque
 * 
 */
public class InputScreen extends MainScreen {
	/**
	 * Fields for entering url, port and number of allowed retries in case of a
	 * failure
	 */
	private EditField efHost, efPort, efRetries;
	/** Action buttons to run the tests and to show/hide advanced options */
	private CustomButtonField bfRun, bfAdvanced;
	/** Fields to enter WAP1.0 parameters */
	private EditField efWapGatewayIP, efWapGatewayAPN, efWapGatewayPort, efWapSourceIP, efWapSourcePort, efWapUser,
			efWapPassword, efTcpAPN, efTcpAPNUser, efTcpAPNPassword;
	private CheckboxField cfTestHTTP, cfTestSocket, cfWapEnableWTLS;
	/** Separator Fields */
	private SeparatorField sf1, sf2, sf3, sf4;
	/** A few LabelFields */
	private LabelField lfTCP, lfWAP, lfSelectTransports;
	/** Fields to choose transports to test */
	private CheckboxField cfTCP, cfMDS, cfBIS, cfWAP, cfWAP2, cfWiFi, cfUnite;
	/** A CommunicationThread which handles all the http I/O */
	private IOThread ct;
	/** An instance of this */
	private InputScreen iscreen;

	/**
	 * Constructor. Initializes all the UI Fields and adds them to this.
	 */
	public InputScreen() {
		this.iscreen = this;
		setTitle("Network Diagnostics");

		sf1 = new SeparatorField();
		sf2 = new SeparatorField();
		sf3 = new SeparatorField();
		sf4 = new SeparatorField();

		efHost = new EditField("Host: ", "http://www.google.ca/search?q=blackberry");
		efPort = new EditField("Port [Optional]: ", "", 7, EditField.FILTER_INTEGER);
		efRetries = new EditField("# of Retries: [Optional]: ", "1", 3, EditField.FILTER_INTEGER);

		cfTestHTTP = new CheckboxField("Test HTTP", true);
		cfTestSocket = new CheckboxField("Test Socket", true);

		bfAdvanced = new CustomButtonField("Advanced", DrawStyle.ELLIPSIS);
		bfRun = new CustomButtonField("Run", DrawStyle.ELLIPSIS);
		HorizontalFieldManager hf = new HorizontalFieldManager();
		hf.add(bfAdvanced);
		hf.add(bfRun);

		lfSelectTransports = new LabelField("Select transports to test:");
		cfTCP = new CheckboxField("  Direct TCP", true);
		cfMDS = new CheckboxField("  MDS", true);
		cfBIS = new CheckboxField("  BIS-B", true);
		cfWAP = new CheckboxField("  WAP", true);
		cfWAP2 = new CheckboxField("  WAP2", true);
		cfWiFi = new CheckboxField("  WiFi", true);
		cfUnite = new CheckboxField("  Unite", true);

		lfTCP = new LabelField("TCP Options:");
		efTcpAPN = new EditField("  APN: ", "");
		efTcpAPNUser = new EditField("  Username: ", "");
		efTcpAPNPassword = new EditField("  Password: ", "");

		lfWAP = new LabelField("WAP Options");
		efWapGatewayAPN = new EditField("  Gateway APN: ", "");
		efWapGatewayIP = new EditField("  Gateway IP: ", "");
		efWapGatewayPort = new EditField("  Gateway Port: ", "");
		efWapSourceIP = new EditField("  Source IP: ", "");
		efWapSourcePort = new EditField("  Source Port: ", "");
		efWapUser = new EditField("  Username: ", "");
		efWapPassword = new EditField("  Password: ", "");
		cfWapEnableWTLS = new CheckboxField("  Enable WTLS", false);

		add(efHost);
		add(efPort);
		add(efRetries);
		add(cfTestHTTP);
		add(cfTestSocket);
		add(sf1);
		add(hf);
		add(sf2);

		populateCarrierInfo();
	}

	private void populateCarrierInfo() {
		final String carrierName = RadioInfo.getCurrentNetworkName();;
		int mcc = -1, mnc = -1;
		CarrierInfo cinfo = null;
		CarrierInfoParser cparser = null;

		try {
			if (RadioInfo.getNetworkType() == RadioInfo.NETWORK_CDMA) {				
				String imsi = GPRSInfo.imeiToString(CDMAInfo.getIMSI());
				mcc = Integer.parseInt(imsi.substring(0, 3));
				mnc = Integer.parseInt(imsi.substring(3, 6));
			} else if (RadioInfo.getNetworkType() == RadioInfo.NETWORK_UMTS
					|| RadioInfo.getNetworkType() == RadioInfo.NETWORK_GPRS) {				
				mcc = Integer.parseInt(Integer.toHexString(GPRSInfo.getHomeMCC()));
				mnc = Integer.parseInt(Integer.toHexString(GPRSInfo.getHomeMNC()));
			}
		} catch (Throwable t) {}
		
		InputStream xmlis=null;
		try{
			FileConnection fconn = (FileConnection)Connector.open("file:///store/netdiag/carrier_info.xml",Connector.READ);
			xmlis = fconn.openInputStream();
		} catch(Exception e){ }
		
		if(xmlis!=null){
			cparser = new CarrierInfoParser(xmlis);
		
			if (mcc != -1 && mnc != -1)
				cinfo = cparser.getCarrierInfo(mcc, mnc);
			else if (mcc != -1 && mnc == -1)
				cinfo = cparser.getCarrierInfo(mcc, carrierName);
			if (cinfo == null && mcc!=-1)
				cinfo = cparser.getCarrierInfo(mcc, carrierName.substring(0, carrierName.indexOf(" ")));
		}
		if(cinfo!=null){
			cinfo.setCarrierName(carrierName);
			
			efTcpAPN.setText(cinfo.getTcpAPN());
			efTcpAPNUser.setText(cinfo.getTcpAPNUserName());
			efTcpAPNPassword.setText(cinfo.getTcpAPNPassword());
			efWapGatewayAPN.setText(cinfo.getWapAPN());
			efWapGatewayIP.setText(cinfo.getWapIP());
			efWapGatewayPort.setText(cinfo.getWapPort());
			efWapSourceIP.setText(cinfo.getWapSourceIP());
			efWapSourcePort.setText(cinfo.getWapSourcePort());
			efWapUser.setText(cinfo.getWapUserName());
			efWapPassword.setText(cinfo.getWapPassword());
			cfWapEnableWTLS.setChecked(cinfo.isEnabaleWTLS());	
			UiApplication.getUiApplication().invokeLater(new Runnable(){
				public void run(){
					Dialog.inform("Your carrier is "+carrierName+". All carrier specific parameters are automatically loaded.");
				}
			});			
		}
	}

	/**
	 * Custom implementation of ButtonField which handles click events for the
	 * Advanced and Run buttons.
	 * 
	 * @author Shadid Haque
	 * 
	 */
	private class CustomButtonField extends ButtonField {

		private CustomButtonField(String label, long style) {
			super(label, style);
		}

		protected boolean navigationClick(int status, int time) {
			if (getLabel().equals("Advanced")) {
				bfAdvanced.setLabel("Hide Options");
				add(lfSelectTransports);
				add(cfTCP);
				add(cfMDS);
				add(cfBIS);
				add(cfWAP);
				add(cfWAP2);
				add(cfWiFi);
				add(cfUnite);
				add(sf3);
				add(lfTCP);
				add(efTcpAPN);
				add(efTcpAPNUser);
				add(efTcpAPNPassword);
				add(sf4);
				add(lfWAP);
				add(efWapGatewayAPN);
				add(efWapGatewayIP);
				add(efWapGatewayPort);
				add(efWapUser);
				add(efWapPassword);
				add(efWapSourceIP);
				add(efWapSourcePort);
				add(cfWapEnableWTLS);
			} else if (getLabel().equals("Hide Options")) {
				bfAdvanced.setLabel("Advanced");
				delete(lfSelectTransports);
				delete(cfTCP);
				delete(cfMDS);
				delete(cfBIS);
				delete(cfWAP);
				delete(cfWAP2);
				delete(cfWiFi);
				delete(cfUnite);
				delete(sf3);
				delete(lfTCP);
				delete(efTcpAPN);
				delete(efTcpAPNUser);
				delete(efTcpAPNPassword);
				delete(sf4);
				delete(lfWAP);
				delete(efWapGatewayAPN);
				delete(efWapGatewayIP);
				delete(efWapGatewayPort);
				delete(efWapUser);
				delete(efWapPassword);
				delete(efWapSourceIP);
				delete(efWapSourcePort);
				delete(cfWapEnableWTLS);
			} else if (getLabel().equals("Run")) {
				ct = new IOThread(iscreen);
				ct.start();
			}
			return true;
		}
	}

	/** Getters and Setters for the UI Fields */
	public String getEfHost() {
		return efHost.getText();
	}

	public String getEfPort() {
		return efPort.getText();
	}

	public String getEfWapGatewayIP() {
		return efWapGatewayIP.getText();
	}

	public String getEfWapGatewayAPN() {
		return efWapGatewayAPN.getText();
	}

	public String getEfWapGatewayPort() {
		return efWapGatewayPort.getText();
	}

	public String getEfWapSourceIP() {
		return efWapSourceIP.getText();
	}

	public String getEfWapSourcePort() {
		return efWapSourcePort.getText();
	}

	public String getEfWapUser() {
		return efWapUser.getText();
	}

	public String getEfWapPassword() {
		return efWapPassword.getText();
	}

	public String getEfTcpAPN() {
		return efTcpAPN.getText();
	}

	public String getEfTcpAPNUser() {
		return efTcpAPNUser.getText();
	}

	public String getEfTcpAPNPassword() {
		return efTcpAPNPassword.getText();
	}

	public boolean getCfWapEnableWTLS() {
		return cfWapEnableWTLS.getChecked();
	}

	public boolean isDoTCP() {
		return cfTCP.getChecked();
	}

	public boolean isDoMDS() {
		return cfMDS.getChecked();
	}

	public boolean isDoBIS() {
		return cfBIS.getChecked();
	}

	public boolean isDoWAP() {
		return cfWAP.getChecked();
	}

	public boolean isDoWAP2() {
		return cfWAP2.getChecked();
	}

	public boolean isDoWiFi() {
		return cfWiFi.getChecked();
	}

	public boolean isDoUnite() {
		return cfUnite.getChecked();
	}

	public int getEfRetries() {
		try {
			int num = Integer.parseInt(efRetries.getText());
			if (num < 1)
				return 1;
			else
				return num;
		} catch (NumberFormatException e) {
			return 1;
		}
	}

	public boolean isTestSocket() {
		return cfTestSocket.getChecked();
	}

	public boolean isTestHTTP() {
		return cfTestHTTP.getChecked();
	}

	/** END OF Getters and Setters */

	protected boolean onSave() {
		return true;
	}

	protected boolean onSavePrompt() {
		return true;
	}

}