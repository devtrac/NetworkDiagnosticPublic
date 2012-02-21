import java.util.Vector;

import net.rim.blackberry.api.mail.Address;
import net.rim.blackberry.api.mail.Folder;
import net.rim.blackberry.api.mail.Message;
import net.rim.blackberry.api.mail.Session;
import net.rim.blackberry.api.mail.Store;
import net.rim.blackberry.api.mail.Transport;
import net.rim.device.api.system.Bitmap;
import net.rim.device.api.system.DeviceInfo;
import net.rim.device.api.system.RadioInfo;
import net.rim.device.api.ui.DrawStyle;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.MenuItem;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.BitmapField;
import net.rim.device.api.ui.component.ButtonField;
import net.rim.device.api.ui.component.EditField;
import net.rim.device.api.ui.component.GaugeField;
import net.rim.device.api.ui.component.LabelField;
import net.rim.device.api.ui.component.SeparatorField;
import net.rim.device.api.ui.container.HorizontalFieldManager;
import net.rim.device.api.ui.container.MainScreen;
import net.rim.device.api.ui.Manager;

/**
 * Shows network/radio information and the progress of each tests over each transport. For each transport test
 * that is finished, shows a Details button which shows the Detailed log of the test for that transport.
 * @author Shadid Haque
 *
 */
public class ReportScreen extends MainScreen{	
	/** A reference to the GaugeField that shows the progress of a single transport test */
	private Field progress;
	/** A Thread which handles updating the value of the GaugeField */
	private Thread tprogress; 
	/** A reference to IOThread instance */
	IOThread ioThread;
	/** 
	 * Each Log passed on to displayResult(Log log) is added to this Vector 
	 * This Vector is used by this Screen when the user wants to email the logs.  
	 */ 
	private Vector logs;
	/** Lets the user enter an email address */
	private EditField emailField, subjectField;
	/** Clicking this button will send the logs via email to emailField.getText() */
	private CustomButtonField sendButton = new CustomButtonField("Send",DrawStyle.ELLIPSIS);

	/**
	 * Constructor. Sets the title of the Screen and displays various network/radio info by calling 
	 * displayNetworkInfo();
	 */
	public ReportScreen(IOThread ioThread) {
		emailField = new EditField("Email: ","",256,EditField.FILTER_EMAIL);
		subjectField = new EditField("Subject: ","");
		sendButton = new CustomButtonField("Send Logs",DrawStyle.ELLIPSIS);
		logs = new Vector();
		this.ioThread = ioThread;		
		setTitle("Diagnostic Report");
		displayNetworkInfo();
		MenuItem miEmail = new MenuItem("Email Report", 1, 1){
			public void run(){
				synchronized(UiApplication.getEventLock()){
					if(emailField.getScreen()!=null)
						delete(emailField);
					if(subjectField.getScreen()!=null)
						delete(subjectField);
					if(sendButton.getScreen()!=null)
						delete(sendButton);
					
					emailField.setLabel("Email: ");
					emailField.setText("");		
				
					add(emailField);
					add(subjectField);
					add(sendButton);
					
					scroll(Manager.BOTTOMMOST);						
					emailField.setFocus();
				}
			}
		};
		addMenuItem(miEmail);
	}
	
	/**
	 * Displays various network/radio info
	 */
	public void displayNetworkInfo(){
		if(RadioInfo.getSignalLevel(RadioInfo.WAF_3GPP)!=RadioInfo.LEVEL_NO_COVERAGE){
			add(new EditField("3GPP Signal Level: ",RadioInfo.getSignalLevel(RadioInfo.WAF_3GPP)+"dBm",255,EditField.READONLY));			
			add(new SeparatorField());
		}
		if(RadioInfo.getSignalLevel(RadioInfo.WAF_CDMA)!=RadioInfo.LEVEL_NO_COVERAGE){
			add(new EditField("CDMA Signal Level: ",RadioInfo.getSignalLevel(RadioInfo.WAF_CDMA)+"dBm",255,EditField.READONLY));
			add(new SeparatorField());
		}
		if(RadioInfo.getSignalLevel(RadioInfo.WAF_IDEN)!=RadioInfo.LEVEL_NO_COVERAGE){
			add(new EditField("iDEN Signal Level: ",RadioInfo.getSignalLevel(RadioInfo.WAF_IDEN)+"dBm",255,EditField.READONLY));
			add(new SeparatorField());
		}
		if(RadioInfo.getSignalLevel(RadioInfo.WAF_WLAN)!=RadioInfo.LEVEL_NO_COVERAGE){
			add(new EditField("WLAN Signal Level: ",RadioInfo.getSignalLevel(RadioInfo.WAF_WLAN)+"dBm",255,EditField.READONLY));
			add(new SeparatorField());
		}		
					
		add(new EditField("Network: ",RadioInfo.getCurrentNetworkName(),255,EditField.READONLY));
		add(new SeparatorField());
		
		String netType="";
		switch(RadioInfo.getNetworkType()){
			case RadioInfo.NETWORK_CDMA:
				netType="CDMA";
				break;
			case RadioInfo.NETWORK_GPRS:
				netType="GPRS";
				break;
			case RadioInfo.NETWORK_IDEN:
				netType="IDEN";
				break;
			case RadioInfo.NETWORK_NONE:
				netType="NONE";
				break;
			case RadioInfo.NETWORK_UMTS:
				netType="UMTS";
				break;
			case RadioInfo.NETWORK_802_11:
				netType="802.11";
				break;
			default:
				netType="Failed to determine";
		}
		add(new EditField("Network Type: ",netType,255,EditField.READONLY));
		add(new SeparatorField());
				
		int activeWAFS = RadioInfo.getNetworkService();
		String services = "";
		if((activeWAFS & RadioInfo.NETWORK_SERVICE_DATA)>0)
			services+="Data + ";
		if((activeWAFS & RadioInfo.NETWORK_SERVICE_DIRECT_CONNECT)>0)
			services+="Direct Connect + ";
		if((activeWAFS & RadioInfo.NETWORK_SERVICE_E911_CALLBACK_MODE)>0)
			services+="E911 Callback + ";
		if((activeWAFS & RadioInfo.NETWORK_SERVICE_EDGE)>0)
			services+="EDGE + ";
		if((activeWAFS & RadioInfo.NETWORK_SERVICE_EMERGENCY_ONLY)>0)
			services+="Emergency Only + ";
		if((activeWAFS & RadioInfo.NETWORK_SERVICE_EVDO)>0 && RadioInfo.getNetworkType()==RadioInfo.NETWORK_CDMA)
			services+="EVDO + ";
		if((activeWAFS & RadioInfo.NETWORK_SERVICE_EVDO_ONLY)>0)
			services+="EVDO Only + ";
		if((activeWAFS & RadioInfo.NETWORK_SERVICE_GAN)>0)
			services+="GAN + ";
		if((activeWAFS & RadioInfo.NETWORK_SERVICE_IN_CITY_ZONE)>0)
			services+="City Zone + ";
		if((activeWAFS & RadioInfo.NETWORK_SERVICE_IN_HOME_ZONE)>0)
			services+="Home Zone + ";
		if((activeWAFS & RadioInfo.NETWORK_SERVICE_MODEM_MODE_ENABLED)>0)
			services+="Modem Mode + ";
		if((activeWAFS & RadioInfo.NETWORK_SERVICE_ROAMING)>0)
			services+="Roaming + ";
		if((activeWAFS & RadioInfo.NETWORK_SERVICE_ROAMING_OFF_CAMPUS)>0)
			services+="Roaming Off-campus + ";
		if((activeWAFS & RadioInfo.NETWORK_SERVICE_SUPPRESS_ROAMING)>0)
			services+="Supress Roaming + ";
		if((activeWAFS & RadioInfo.NETWORK_SERVICE_UMTS)>0)
			services+="UMTS + ";
		if((activeWAFS & RadioInfo.NETWORK_SERVICE_VOICE)>0)
			services+="Voice + ";
		if(services.length()>2){
			add(new EditField("Network Services: ",services.substring(0,services.length()-2),255,EditField.READONLY));
			add(new SeparatorField());
		} else{
			add(new EditField("Network Services: ","No Service Found",255,EditField.READONLY));
			add(new SeparatorField());
		}
		
		add(new EditField("PIN: ",Integer.toHexString(DeviceInfo.getDeviceId())+"",255,EditField.READONLY));
		add(new SeparatorField());
		
		add(new EditField("Battery: ",DeviceInfo.getBatteryLevel()+"%",255,EditField.READONLY));
		add(new SeparatorField());			
		
		add(new SeparatorField());
	}

	/**
	 * Displays a pass or fail image for a Transport test. A CustomButtonField titled "Details" is 
	 * also shown, clicking on which displays a very detailed report of a transport's test.
	 * @param log	The Log instance for the transport in question
	 */
	public void displayResult(final Log log) {
		logs.addElement(log);
		UiApplication.getUiApplication().invokeLater(new Runnable() {
			public void run() {
				if(tprogress!=null)					
					tprogress.interrupt();				
				if(progress!=null && progress.getScreen()!=null)
					delete(progress);			

				HorizontalFieldManager hfm = new HorizontalFieldManager();
				hfm.add(new CustomButtonField("Details", DrawStyle.ELLIPSIS,log));			
				hfm.add(new LabelField("\t"+log.getTransport() + ": "));
				BitmapField bf = new BitmapField(Bitmap.getBitmapResource(log.isPass() ? "pass.JPG" : "fail.JPG"));
				hfm.add(bf);
				add(hfm);
				
				add(new SeparatorField());
			}
		});
	}

	/**
	 * Displays a GaugeField and keeps incrementing the progress at fixed interval until the test succeeds
	 * or gives up after 'retries' number of failures.
	 * @param transport	Name of the transport test this progress is for
	 */
	public void displayProgress(final String transport) {
		UiApplication.getUiApplication().invokeLater(new Runnable() {
			public void run() {				
				GaugeField p = new GaugeField(transport+": ",0,100,0,GaugeField.NO_TEXT); 
				add(p);
				progress = p;				
			}
		});
		tprogress = new Thread() {
			public void run() {
				while (true){
					UiApplication.getUiApplication().invokeLater(new Runnable() {
						public void run() {
							if(((GaugeField)progress).getValue()>99)
								((GaugeField)progress).setValue(0);
							((GaugeField)progress).setValue(((GaugeField)progress).getValue()+1);
						}
					});
					try {
						sleep(500);
					} catch (InterruptedException e) {}
				}
			}
		};
		tprogress.start();
	}
	
	/**
	 * Sets the value of the current attempt which is displayed on the Screen.
	 * @param currentAttempt	current attempt
	 * @param retries	number of allowed retries.
	 */
	public void setTrial(final int currentAttempt, final int retries){
		UiApplication.getUiApplication().invokeLater(new Runnable() {
			public void run() {		
				((GaugeField)progress).setLabel(((GaugeField)progress).getLabel().substring(0,((GaugeField)progress).getLabel().indexOf(":")+2)+"Attempt "+(currentAttempt+1)+"/"+retries);
			}
		});
	}

	/**
	 * Custom implementation of ButtonField which handles click events for the Details button.
	 * If the user clicks on Details, this will show all the details of a single transport
	 * test on a separate Screen created on the fly.
	 * @author Shadid Haque
	 *
	 */
	private class CustomButtonField extends ButtonField {
		private Log log;

		private CustomButtonField(String label, long style, Log log) {
			super(label, style);
			this.log = log;
		}
		private CustomButtonField(String label, long style) {
			super(label, style);			
		}

		private MainScreen buildDetails() {
			MainScreen ms = new MainScreen();
			ms.setTitle("Detailed Report: "+log.getTransport());
			ms.add(new SeparatorField());
			ms.add(new SeparatorField());

						
			ms.add(new EditField("Transport: ", log.getTransport(),255,EditField.READONLY));
			ms.add(new EditField("Result: ", log.isPass() ? "Pass" : "Fail",255,EditField.READONLY));
			ms.add(new EditField("Response: ", "" + log.getResponseCode(),255,EditField.READONLY));
			ms.add(new EditField("Length: ", "" + log.getContentLength(),255,EditField.READONLY));						
			
			ms.add(new SeparatorField());
			
			ms.add(new EditField("URL: ", log.getUrl(),255,EditField.READONLY));
			ms.add(new SeparatorField());
			ms.add(new EditField("Log: ", "\n" + log.getLog()));
			ms.add(new SeparatorField());
			ms.add(new EditField("Content: ", "\n" + log.getContent()));
			ms.add(new SeparatorField());
			ms.add(new SeparatorField());
			return ms;
		}

		protected boolean navigationClick(int status, int time) {
			if (this.getLabel().equalsIgnoreCase("Details")){
				UiApplication.getUiApplication().invokeLater(new Runnable() {
					public void run() {
						UiApplication.getUiApplication().pushScreen(buildDetails());
					}
				});
			} else if(this.getLabel().equalsIgnoreCase("Send Logs")){
				sendEmail();
			}

			return true;
		}
	}
	
	/**
	 * Sends the logs by email
	 * @return	true if sent successfully
	 */
	public boolean sendEmail(){
		try{
			String emailText = "";
			for(int i=0; i<logs.size(); i++){
				Log l = (Log)logs.elementAt(i);
				emailText+=l.toString();
			}		
			Session session = Session.getDefaultInstance();
			Store store = session.getStore();			
			Folder sentFolder = store.getFolder(Folder.SENT);
			Message msg = new Message(sentFolder);
			Address[] addrs = new Address[1]; 
            addrs[0] = new Address(emailField.getText(), ""); 
            msg.addRecipients(Message.RecipientType.TO, addrs);
            msg.setSubject(subjectField.getText());
            msg.setContent(emailText);
            Transport.send(msg); 
            emailField.setLabel("Logs sent to "+emailField.getText()+"!");
            emailField.setText("");
            delete(sendButton);
            delete(subjectField);
		} catch(Throwable t){
			ioThread.addToAllLogs(t.getMessage());
		}
				
			
		return false;
	}

	public boolean onClose() {
		ioThread=null;
		return super.onClose();
	}

	protected boolean onSave() {		
		return true;
	}

	protected boolean onSavePrompt() {
		return true;
	}
	
	
	
}