
public class CarrierInfo {
	private String carrierName;
	private String country;
	private String mcc;
	private String mnc;
	
	private String tcpAPN;
	private String tcpAPNUserName;
	private String tcpAPNPassword;
	
	private String wapAPN;
	private String wapIP;
	private String wapPort;
	private String wapUserName;
	private String wapPassword;
	private String wapSourceIP;
	private String wapSourcePort;
	private boolean isEnabaleWTLS;
	
	
	
	public CarrierInfo() {
		this.carrierName="";
		this.country="";
		this.mcc="";
		this.mnc="";
		
		this.tcpAPN = "";
		this.tcpAPNUserName = "";
		this.tcpAPNPassword = "";
		this.wapAPN = "";
		this.wapIP = "";
		this.wapPort = "";
		this.wapUserName = "";
		this.wapPassword = "";
		this.wapSourceIP = "";
		this.wapSourcePort = "";
		this.isEnabaleWTLS = false;
		
	}
	public String getTcpAPN() {
		return tcpAPN;
	}
	public void setTcpAPN(String tcpAPN) {
		this.tcpAPN = tcpAPN;
	}
	public String getTcpAPNUserName() {
		return tcpAPNUserName;
	}
	public void setTcpAPNUserName(String tcpAPNUserName) {
		this.tcpAPNUserName = tcpAPNUserName;
	}
	public String getTcpAPNPassword() {
		return tcpAPNPassword;
	}
	public void setTcpAPNPassword(String tcpAPNPassword) {
		this.tcpAPNPassword = tcpAPNPassword;
	}
	public String getWapAPN() {
		return wapAPN;
	}
	public void setWapAPN(String wapAPN) {
		this.wapAPN = wapAPN;
	}
	public String getWapIP() {
		return wapIP;
	}
	public void setWapIP(String wapIP) {
		this.wapIP = wapIP;
	}
	public String getWapPort() {
		return wapPort;
	}
	public void setWapPort(String wapPort) {
		this.wapPort = wapPort;
	}
	public String getWapUserName() {
		return wapUserName;
	}
	public void setWapUserName(String wapUserName) {
		this.wapUserName = wapUserName;
	}
	public String getWapPassword() {
		return wapPassword;
	}
	public void setWapPassword(String wapPassword) {
		this.wapPassword = wapPassword;
	}
	public String getWapSourceIP() {
		return wapSourceIP;
	}
	public void setWapSourceIP(String wapSourceIP) {
		this.wapSourceIP = wapSourceIP;
	}
	public String getWapSourcePort() {
		return wapSourcePort;
	}
	public void setWapSourcePort(String wapSourcePort) {
		this.wapSourcePort = wapSourcePort;
	}
	public boolean isEnabaleWTLS() {
		return isEnabaleWTLS;
	}
	public void setEnabaleWTLS(boolean isEnabaleWTLS) {
		this.isEnabaleWTLS = isEnabaleWTLS;
	}
	
	public String toString(){
		StringBuffer sb = new StringBuffer();
		sb.append("\nCountry: "+country);
		sb.append("\nMCC: "+mcc);
		sb.append("\nCarrier: "+carrierName);
		sb.append("\nMNC: "+mnc);
		sb.append("\ntcpAPN: "+tcpAPN);
		sb.append("\ntcpAPNUserName: "+tcpAPNUserName);
		sb.append("\ntcpAPNPassword: "+tcpAPNPassword);
		sb.append("\nwapAPN: "+wapAPN);
		sb.append("\nwapIP: "+wapIP);
		sb.append("\nwapPort: "+wapPort);
		sb.append("\nwapUserName: "+wapUserName);
		sb.append("\nwapPassword: "+wapPassword);
		sb.append("\nwapSourceIP: "+wapSourceIP);
		sb.append("\nwapSourcePort: "+wapSourcePort);
		sb.append("\nisEnabaleWTLS: "+isEnabaleWTLS);
		
		
		return sb.toString();
	}
	public String getCarrierName() {
		return carrierName;
	}
	public void setCarrierName(String carrierName) {
		this.carrierName = carrierName;
	}
	public String getCountry() {
		return country;
	}
	public void setCountry(String country) {
		this.country = country;
	}
	public String getMcc() {
		return mcc;
	}
	public void setMcc(String mcc) {
		this.mcc = mcc;
	}
	public String getMnc() {
		return mnc;
	}
	public void setMnc(String mnc) {
		this.mnc = mnc;
	}
	
	
}
