import net.rim.device.api.xml.parsers.*;

import org.w3c.dom.*;

import java.io.*;

public class CarrierInfoParser {	
	private DocumentBuilderFactory factory;
	private DocumentBuilder builder;
	private InputStream inputStream;
	private Document document;	

	public CarrierInfoParser(InputStream is) {
		
		try {
			factory = DocumentBuilderFactory.newInstance();
			builder = factory.newDocumentBuilder();
			inputStream = is;
			document = builder.parse(inputStream);
		} catch (Throwable t) {

		}
	}
	
	/**
	 * Returns a CarrierInfo object if a <carrier> is found that matches the search keys, 'mcc' and 'carrierName'
	 * @param mcc	MCC search key
	 * @param carrierName	Carrier name to search for 
	 * @return	A CarrierInfo object if the search has a result. null otherwise.
	 */
	public CarrierInfo getCarrierInfo(int mcc, String carrierName) {
		CarrierInfo cinfo = new CarrierInfo();
		// parse root
		Element rootNode = document.getDocumentElement();
		rootNode.normalize();	
		
		// parse <country>'s
		NodeList countryList = rootNode.getChildNodes();


		for (int i=0; i<countryList.getLength(); i++) {
			Node curCountry = countryList.item(i);
			if(curCountry.getNodeType()==Node.ELEMENT_NODE && curCountry.getNodeName().equals("country")){
				// parse <country> attributes								
				NamedNodeMap countryAtts = curCountry.getAttributes();
				for(int j=0; j<countryAtts.getLength(); j++){
					String countryAttName = countryAtts.item(j).getNodeName();
					String countryAttValue = countryAtts.item(j).getNodeValue();	
					if(countryAttName.equals("name"))
						cinfo.setCountry(countryAttValue);
					if(countryAttName.equals("mcc"))
						cinfo.setMcc(countryAttValue);
					if(countryAttName.equals("mcc") && countryAttValue.equals(Integer.toString(mcc))){
						// parse this <country>
						NodeList carrierList = curCountry.getChildNodes();
						for(int k=0; k<carrierList.getLength(); k++){
							Node curCarrier = carrierList.item(k);
							if(curCarrier.getNodeType()==Node.ELEMENT_NODE && curCarrier.getNodeName().equals("carrier")){
								// parse carrier attributes
								NamedNodeMap carrierAtts = curCarrier.getAttributes();								
								for(int l=0; l<carrierAtts.getLength(); l++){
									String carrierAttName = carrierAtts.item(l).getNodeName();
									String carrierAttValue = carrierAtts.item(l).getNodeValue();
									if(carrierAttName.equals("name")){
										cinfo.setCarrierName(carrierAttValue);									
										cinfo.setMnc(carrierAtts.item(l+1).getNodeValue());
									}									
									if(carrierAttName.equals("name") && carrierAttValue.toLowerCase().indexOf(carrierName.toLowerCase())!=-1){
										// parse this carrier
										NodeList carrierData = curCarrier.getChildNodes();
										for(int m=0; m<carrierData.getLength(); m++){
											Node curDataNode = carrierData.item(m);
											if(curDataNode.getNodeType()==Node.ELEMENT_NODE && curDataNode.getNodeName().equals("tcpAPN")){
												if(curDataNode.hasChildNodes())
													cinfo.setTcpAPN(curDataNode.getFirstChild().getNodeValue()!=null?curDataNode.getFirstChild().getNodeValue():"");												
											}else if(curDataNode.getNodeType()==Node.ELEMENT_NODE && curDataNode.getNodeName().equals("tcpAPNUserName")){
												if(curDataNode.hasChildNodes())
													cinfo.setTcpAPNUserName(curDataNode.getFirstChild().getNodeValue()!=null?curDataNode.getFirstChild().getNodeValue():"");												
											}else if(curDataNode.getNodeType()==Node.ELEMENT_NODE && curDataNode.getNodeName().equals("tcpAPNPassword")){
												if(curDataNode.hasChildNodes())
													cinfo.setTcpAPNPassword(curDataNode.getFirstChild().getNodeValue()!=null?curDataNode.getFirstChild().getNodeValue():"");												
											}else if(curDataNode.getNodeType()==Node.ELEMENT_NODE && curDataNode.getNodeName().equals("wapAPN")){
												if(curDataNode.hasChildNodes())
													cinfo.setWapAPN(curDataNode.getFirstChild().getNodeValue()!=null?curDataNode.getFirstChild().getNodeValue():"");												
											}else if(curDataNode.getNodeType()==Node.ELEMENT_NODE && curDataNode.getNodeName().equals("wapIP")){
												if(curDataNode.hasChildNodes())
													cinfo.setWapIP(curDataNode.getFirstChild().getNodeValue()!=null?curDataNode.getFirstChild().getNodeValue():"");												
											}else if(curDataNode.getNodeType()==Node.ELEMENT_NODE && curDataNode.getNodeName().equals("wapPort")){
												if(curDataNode.hasChildNodes())
													cinfo.setWapPort(curDataNode.getFirstChild().getNodeValue()!=null?curDataNode.getFirstChild().getNodeValue():"");												
											}else if(curDataNode.getNodeType()==Node.ELEMENT_NODE && curDataNode.getNodeName().equals("wapUserName")){
												if(curDataNode.hasChildNodes())
													cinfo.setWapUserName(curDataNode.getFirstChild().getNodeValue()!=null?curDataNode.getFirstChild().getNodeValue():"");												
											}else if(curDataNode.getNodeType()==Node.ELEMENT_NODE && curDataNode.getNodeName().equals("wapPassword")){
												if(curDataNode.hasChildNodes())
													cinfo.setWapPassword(curDataNode.getFirstChild().getNodeValue()!=null?curDataNode.getFirstChild().getNodeValue():"");												
											}else if(curDataNode.getNodeType()==Node.ELEMENT_NODE && curDataNode.getNodeName().equals("wapSourceIP")){
												if(curDataNode.hasChildNodes())
													cinfo.setWapSourceIP(curDataNode.getFirstChild().getNodeValue()!=null?curDataNode.getFirstChild().getNodeValue():"");												
											}else if(curDataNode.getNodeType()==Node.ELEMENT_NODE && curDataNode.getNodeName().equals("wapSourcePort")){
												if(curDataNode.hasChildNodes())
													cinfo.setWapSourcePort(curDataNode.getFirstChild().getNodeValue()!=null?curDataNode.getFirstChild().getNodeValue():"");												
											}else if(curDataNode.getNodeType()==Node.ELEMENT_NODE && curDataNode.getNodeName().equals("isEnabaleWTLS")){
												if(curDataNode.hasChildNodes())
													cinfo.setEnabaleWTLS(curDataNode.getFirstChild().getNodeValue()!=null && curDataNode.getFirstChild().getNodeValue().equals("true")?true:false);												
											}
										}
										return cinfo;
									}
								}
							}
						}
					}					
				}
			}	
		}
		return null;
	}

	/**
	 * Returns a CarrierInfo object if a <carrier> is found that matches the search keys, 'mcc' and 'carrierName'
	 * @param mcc	MCC search key
	 * @param carrierName	Carrier name to search for 
	 * @return	A CarrierInfo object if the search has a result. null otherwise.
	 */
	public CarrierInfo getCarrierInfo(int mcc, int mnc) {
		CarrierInfo cinfo = new CarrierInfo();
		// parse root
		Element rootNode = document.getDocumentElement();
		rootNode.normalize();
		
		String rootNodeName = rootNode.getNodeName();		
		
		// parse <country>'s
		NodeList countryList = rootNode.getChildNodes();

		for (int i=0; i<countryList.getLength(); i++) {
			Node curCountry = countryList.item(i);
			if(curCountry.getNodeType()==Node.ELEMENT_NODE && curCountry.getNodeName().equals("country")){
				// parse <country> attributes								
				NamedNodeMap countryAtts = curCountry.getAttributes();
				for(int j=0; j<countryAtts.getLength(); j++){
					String countryAttName = countryAtts.item(j).getNodeName();
					String countryAttValue = countryAtts.item(j).getNodeValue();	
					if(countryAttName.equals("name"))
						cinfo.setCountry(countryAttValue);
					if(countryAttName.equals("mcc"))
						cinfo.setMcc(countryAttValue);
					if(countryAttName.equals("mcc") && countryAttValue.equals(Integer.toString(mcc))){
						// parse this <country>
						NodeList carrierList = curCountry.getChildNodes();
						for(int k=0; k<carrierList.getLength(); k++){
							Node curCarrier = carrierList.item(k);
							if(curCarrier.getNodeType()==Node.ELEMENT_NODE && curCarrier.getNodeName().equals("carrier")){
								// parse carrier attributes
								NamedNodeMap carrierAtts = curCarrier.getAttributes();								
								for(int l=0; l<carrierAtts.getLength(); l++){
									String carrierAttName = carrierAtts.item(l).getNodeName();
									String carrierAttValue = carrierAtts.item(l).getNodeValue();
									if(carrierAttName.equals("name")){
										cinfo.setCarrierName(carrierAttValue);									
									}	
									if(carrierAttName.equals("mnc")){																			
										cinfo.setMnc(carrierAttValue);
									}	
									if(carrierAttName.equals("mnc") && carrierAttValue.equals(Integer.toString(mnc))){
										// parse this carrier
										NodeList carrierData = curCarrier.getChildNodes();
										for(int m=0; m<carrierData.getLength(); m++){
											Node curDataNode = carrierData.item(m);
											if(curDataNode.getNodeType()==Node.ELEMENT_NODE && curDataNode.getNodeName().equals("tcpAPN")){
												if(curDataNode.hasChildNodes())
													cinfo.setTcpAPN(curDataNode.getFirstChild().getNodeValue()!=null?curDataNode.getFirstChild().getNodeValue():"");												
											}else if(curDataNode.getNodeType()==Node.ELEMENT_NODE && curDataNode.getNodeName().equals("tcpAPNUserName")){
												if(curDataNode.hasChildNodes())
													cinfo.setTcpAPNUserName(curDataNode.getFirstChild().getNodeValue()!=null?curDataNode.getFirstChild().getNodeValue():"");												
											}else if(curDataNode.getNodeType()==Node.ELEMENT_NODE && curDataNode.getNodeName().equals("tcpAPNPassword")){
												if(curDataNode.hasChildNodes())
													cinfo.setTcpAPNPassword(curDataNode.getFirstChild().getNodeValue()!=null?curDataNode.getFirstChild().getNodeValue():"");												
											}else if(curDataNode.getNodeType()==Node.ELEMENT_NODE && curDataNode.getNodeName().equals("wapAPN")){
												if(curDataNode.hasChildNodes())
													cinfo.setWapAPN(curDataNode.getFirstChild().getNodeValue()!=null?curDataNode.getFirstChild().getNodeValue():"");												
											}else if(curDataNode.getNodeType()==Node.ELEMENT_NODE && curDataNode.getNodeName().equals("wapIP")){
												if(curDataNode.hasChildNodes())
													cinfo.setWapIP(curDataNode.getFirstChild().getNodeValue()!=null?curDataNode.getFirstChild().getNodeValue():"");												
											}else if(curDataNode.getNodeType()==Node.ELEMENT_NODE && curDataNode.getNodeName().equals("wapPort")){
												if(curDataNode.hasChildNodes())
													cinfo.setWapPort(curDataNode.getFirstChild().getNodeValue()!=null?curDataNode.getFirstChild().getNodeValue():"");												
											}else if(curDataNode.getNodeType()==Node.ELEMENT_NODE && curDataNode.getNodeName().equals("wapUserName")){
												if(curDataNode.hasChildNodes())
													cinfo.setWapUserName(curDataNode.getFirstChild().getNodeValue()!=null?curDataNode.getFirstChild().getNodeValue():"");												
											}else if(curDataNode.getNodeType()==Node.ELEMENT_NODE && curDataNode.getNodeName().equals("wapPassword")){
												if(curDataNode.hasChildNodes())
													cinfo.setWapPassword(curDataNode.getFirstChild().getNodeValue()!=null?curDataNode.getFirstChild().getNodeValue():"");												
											}else if(curDataNode.getNodeType()==Node.ELEMENT_NODE && curDataNode.getNodeName().equals("wapSourceIP")){
												if(curDataNode.hasChildNodes())
													cinfo.setWapSourceIP(curDataNode.getFirstChild().getNodeValue()!=null?curDataNode.getFirstChild().getNodeValue():"");												
											}else if(curDataNode.getNodeType()==Node.ELEMENT_NODE && curDataNode.getNodeName().equals("wapSourcePort")){
												if(curDataNode.hasChildNodes())
													cinfo.setWapSourcePort(curDataNode.getFirstChild().getNodeValue()!=null?curDataNode.getFirstChild().getNodeValue():"");												
											}else if(curDataNode.getNodeType()==Node.ELEMENT_NODE && curDataNode.getNodeName().equals("isEnabaleWTLS")){
												if(curDataNode.hasChildNodes())
													cinfo.setEnabaleWTLS(curDataNode.getFirstChild().getNodeValue()!=null && curDataNode.getFirstChild().getNodeValue().equals("true")?true:false);												
											}
										}
										return cinfo;
									}
								}
							}
						}
					}					
				}
			}	
		}
		return null;
	}

}
