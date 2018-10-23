package client;


import java.io.IOException;
import java.net.*;
import java.text.ParseException;
import java.util.*;

import javax.sdp.SdpFactory;
import javax.sdp.SessionDescription;
import javax.sip.address.*;
import javax.sip.header.*;
import javax.sip.message.*;
import javax.sip.*;



public class SipClient  implements SipListener, Runnable {
	

	
	//Objects used to communicate to SIP API
	SipFactory sipFactory; 		// used to access the SIP API
	SipStack sipStack; 			// The sip Stack
	SipProvider sipProvider; 	// used to send sip messages
	MessageFactory messageFactory;// used to create sip message factory
	HeaderFactory headerFactory;// used to create sip headers
	AddressFactory addressFactory;//used to create sip uri
	ListeningPoint listeningPoint;// sip listening IP adress/port
	Properties properties; // other properties
    Dialog dialog;			// dialog
    
   


	//objects keeping local configuration
	String ip; //the local IP address
	int port = 6060; //the local port
	String protocol = "udp"; //local protocol
	int tag = (new Random()).nextInt(); // the local tag
	Address contactAddress; //the contact address
	ContactHeader contactHeader; // the contact header
	
	String AddressTo = "sip:someone@"+ this.ip +":5060";
	
	//constructor
	SipClient()
	{
		this.initialize();
	}
	
	public void run()
	{	
		//sendinfo("sip:audio@192.168.1.101:7060","Please send me the media list");
	}
	
	private static SipClient sipClient = null;

	public static SipClient getSipClient(){
		try{
			if (sipClient == null){
				sipClient = new SipClient();
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}
		return sipClient;
	}
	
	public void initialize(){  // initializing  SIP API
		try 
		{
			
			//get the local IP address
			this.ip = InetAddress.getLocalHost().getHostAddress();
			// create the sip factory and set the path name
			this.sipFactory = SipFactory.getInstance();
			this.sipFactory.setPathName("gov.nist");
			// create and set the sip stack properties
			this.properties = new Properties();
			this.properties.setProperty("javax.sip.STACK_NAME", "stack");
			//create the sip stack
			this.sipStack = this.sipFactory.createSipStack(this.properties);
			// create the sip message factory
			this.messageFactory = this.sipFactory.createMessageFactory();
			//create the Sip header factory
			this.headerFactory = this.sipFactory.createHeaderFactory();
			// create the sip address factory
			this.addressFactory = this.sipFactory.createAddressFactory();
			//create the sip listening point and bind it to the local ip address, port and protocol
			this.listeningPoint = this.sipStack.createListeningPoint(this.ip, this.port, this.protocol);
			// create the sip provider
			this.sipProvider = this.sipStack.createSipProvider(this.listeningPoint);
			// add our application as a SIP listener
			this.sipProvider.addSipListener(this);
			// create the contact address used for all SIP messages
			this.contactAddress = this.addressFactory.createAddress("sip:" + "ApplicationServer" + "@" + this.ip +":" + this.port);
			//create the contact header used for all sip messages
			this.contactHeader = this.headerFactory.createContactHeader(contactAddress);
			
			//display the local IP adress and port
			System.out.println("Local address. " + this.ip + ":" + this.port);
			
	
		}
		catch(Exception e)
		{
			System.out.println("error: "+ e.getMessage());
		}
	}
	
	

	public void sendinvite(Request request,String sdpData,String RequestUri) 
	{
		String loggerdetails=null;
		Request newRequest = (Request) request.clone();	
		try {
			ViaHeader viaHeader = this.headerFactory.createViaHeader(this.ip,  this.port, "udp", null);
			newRequest.addFirst(viaHeader);
			
			
			//set new sdp content
			newRequest.removeContent();
			ContentTypeHeader contentTypeHeader = headerFactory.createContentTypeHeader("application", "sdp");
			newRequest.setContent(sdpData, contentTypeHeader);
			
			// set new request uri
			Address reqURI = this.addressFactory.createAddress(RequestUri);
		    javax.sip.address.URI requestURI = reqURI.getURI();
		    newRequest.setRequestURI(requestURI);
			
		    
			 ClientTransaction clientTransaction= sipProvider.getNewClientTransaction(newRequest);
			clientTransaction.sendRequest();
			//sipProvider.sendRequest(newRequest);
			//System.out.println("Request sent: " + request.getHeader("CSeq"));
			System.out.println(newRequest);
			
		}
		catch (ParseException | InvalidArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NullPointerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SipException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
	}
	

	public void sendMessage(ArrayList<String> message, String toURI)
	{
		try
		{
			//create destination address
			Address addressTo = this.addressFactory.createAddress(toURI);
			// create the request URI for the SIP message
			javax.sip.address.URI requestURI = addressTo.getURI();
			
			// create the sip message headers
			
			// via header
			ArrayList viaHeaders = new ArrayList();
			ViaHeader viaHeader = this.headerFactory.createViaHeader(this.ip,  this.port, "udp", null);
			viaHeaders.add(viaHeader);
			// the max forward header
			MaxForwardsHeader maxForwardsHeader = this.headerFactory.createMaxForwardsHeader(70);
			// call id header
			CallIdHeader callIdHeader = this.sipProvider.getNewCallId();
			// the CSeq header
			//CSeqHeader cSeqHeader = this.headerFactory.createCSeqHeader(1, Request.MESSAGE);
			CSeqHeader cSeqHeader = this.headerFactory.createCSeqHeader(1L, "MESSAGE");
			// the from header
			FromHeader fromHeader = this.headerFactory.createFromHeader(this.contactAddress, String.valueOf(this.tag));
			// To header
			ToHeader toHeader = this.headerFactory.createToHeader(addressTo, null);
			
			//create Message request
			Request request = this.messageFactory.createRequest(
					requestURI,
					"MESSAGE",
					callIdHeader,
					cSeqHeader,
					fromHeader,
					toHeader,
					viaHeaders,
					maxForwardsHeader);
			// add contact header to request		
			request.addHeader(contactHeader);
			
			//add the content of the message
	        ContentTypeHeader contentTypeHeader =
	        headerFactory.createContentTypeHeader("text", "plain");       
	        request.setContent(message, contentTypeHeader);
			
			// send the request statelessly through the SIP provider
	        
			//ClientTransaction tid = sipProvider.getNewClientTransaction(request);  // change starts here
			//dialog = tid.getDialog();
			//dialog.sendRequest(tid);												//change ends here
				this.sipProvider.sendRequest(request);
			
			// display message
			// System.out.println("Request sent. \n" + request.toString());
			System.out.println("Textmessage sent: " + request.getHeader("CSeq"));
			
			// log event
			try {
				Logger logger = Logger.getLogger();
				logger.writetoLogFile(request, "SipClient.sendMessage", "send", "sending an Sip IM", "");
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
		catch(Exception e)
		{
			// if an error occured display the error
			System.out.println("Textmessage sent failed: " + e.getMessage());
		}
	
	}
	
	public void sendMessage(String message, String toURI) {
		// TODO Auto-generated method stub
		
		try
		{
			//create destination address
			Address addressTo = this.addressFactory.createAddress(toURI);
			// create the request URI for the SIP message
			javax.sip.address.URI requestURI = addressTo.getURI();
			
			// create the sip message headers
			
			// via header
			ArrayList viaHeaders = new ArrayList();
			ViaHeader viaHeader = this.headerFactory.createViaHeader(this.ip,  this.port, "udp", null);
			viaHeaders.add(viaHeader);
			// the max forward header
			MaxForwardsHeader maxForwardsHeader = this.headerFactory.createMaxForwardsHeader(70);
			// call id header
			CallIdHeader callIdHeader = this.sipProvider.getNewCallId();
			// the CSeq header
			//CSeqHeader cSeqHeader = this.headerFactory.createCSeqHeader(1, Request.MESSAGE);
			CSeqHeader cSeqHeader = this.headerFactory.createCSeqHeader(1L, "MESSAGE");
			// the from header
			FromHeader fromHeader = this.headerFactory.createFromHeader(this.contactAddress, String.valueOf(this.tag));
			// To header
			ToHeader toHeader = this.headerFactory.createToHeader(addressTo, null);
			
			//create Message request
			Request request = this.messageFactory.createRequest(
					requestURI,
					"MESSAGE",
					callIdHeader,
					cSeqHeader,
					fromHeader,
					toHeader,
					viaHeaders,
					maxForwardsHeader);
			// add contact header to request		
			request.addHeader(contactHeader);
			
			//add the content of the message
	        ContentTypeHeader contentTypeHeader =
	        headerFactory.createContentTypeHeader("text", "plain");       
	        request.setContent(message, contentTypeHeader);
			
			// send the request statelessly through the SIP provider
	        
			//ClientTransaction tid = sipProvider.getNewClientTransaction(request);  // change starts here
			//dialog = tid.getDialog();
			//dialog.sendRequest(tid);												//change ends here
				this.sipProvider.sendRequest(request);
			
			// display message
			// System.out.println("Request sent. \n" + request.toString());
			System.out.println("Textmessage sent: " + request.getHeader("CSeq"));
			
			// log event
			try {
				Logger logger = Logger.getLogger();
				logger.writetoLogFile(request, "SipClient.sendMessage", "send", "sending an Sip IM", "");
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
		catch(Exception e)
		{
			// if an error occured display the error
			System.out.println("Textmessage sent failed: " + e.getMessage());
		}
	
		
	}
	
	public void sendinfo(String RequestURI, String msmlcontent, Request requestinput)
	{
		
		try
		{

		
			//create destination address
			Address addressTo = this.addressFactory.createAddress(RequestURI);
			// create the request URI for the SIP message
			javax.sip.address.URI requestURI = addressTo.getURI();

			// create the sip message headers

			// via header
			ArrayList viaHeaders = new ArrayList();
			ViaHeader viaHeader = this.headerFactory.createViaHeader(this.ip, this.port, "udp", null);
			viaHeaders.add(viaHeader);
		
			// the max forward header
			MaxForwardsHeader maxForwardsHeader = this.headerFactory.createMaxForwardsHeader(70);
			// call id header
			CallIdHeader callIdHeader = this.sipProvider.getNewCallId();
			// the CSeq header
			CSeqHeader cSeqHeader = this.headerFactory.createCSeqHeader(1L, "INFO");
			// the from header
			FromHeader fromHeader = (FromHeader)requestinput.getHeader("From");
			// To header
			ToHeader toHeader = (ToHeader)requestinput.getHeader("To");

			//create Info request
			Request request = this.messageFactory.createRequest(
					requestURI,
					"INFO",
					callIdHeader,
					cSeqHeader,
					fromHeader,
					toHeader,
					viaHeaders,
					maxForwardsHeader);
	
			ContentTypeHeader contentTypeHeader = headerFactory.createContentTypeHeader("application", "msml+xml");


			request.setContent(msmlcontent, contentTypeHeader);
			this.sipProvider.sendRequest(request);

			System.out.println("INFO request sent: " + request.getHeader("CSeq"));

			System.out.println(request);

		}
		catch(Exception e)
		{
			System.out.println("INFO request sent failed: " + e.getMessage());
		}

	}
	
	
	
	public void sendinfo(String toURI, String msmlcontent) 
	{
		try
		{
			//create destination address
			Address addressTo = this.addressFactory.createAddress(toURI);
			// create the request URI for the SIP message
			javax.sip.address.URI requestURI = addressTo.getURI();
			
			// create the sip message headers
			
			// via header
			ArrayList viaHeaders = new ArrayList();
			ViaHeader viaHeader = this.headerFactory.createViaHeader(this.ip,  this.port, "udp", null);
			viaHeaders.add(viaHeader);
			// the max forward header
			MaxForwardsHeader maxForwardsHeader = this.headerFactory.createMaxForwardsHeader(70);
			// call id header
			CallIdHeader callIdHeader = this.sipProvider.getNewCallId();
			// the CSeq header
			CSeqHeader cSeqHeader = this.headerFactory.createCSeqHeader(1L, "INFO");
			// the from header
			FromHeader fromHeader = this.headerFactory.createFromHeader(this.contactAddress, String.valueOf(this.tag));
			// To header
			ToHeader toHeader = this.headerFactory.createToHeader(addressTo, null);
			
			//create Info request
			Request request = this.messageFactory.createRequest(
					requestURI,
					"INFO",
					callIdHeader,
					cSeqHeader,
					fromHeader,
					toHeader,
					viaHeaders,
					maxForwardsHeader);
			// add contact header to request		
			request.addHeader(contactHeader);
			ContentTypeHeader contentTypeHeader = headerFactory.createContentTypeHeader("application", "msml+xml");
			
			
			request.setContent(msmlcontent, contentTypeHeader);
			this.sipProvider.sendRequest(request);

			System.out.println("INFO request sent: " + request.getHeader("CSeq"));
			System.out.println(request);
			
		}
		catch(Exception e)
		{
			System.out.println("INFO request sent failed: " + e.getMessage());
		}
		
	}

	
	@Override
	public void processDialogTerminated(DialogTerminatedEvent arg0) {

		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void processIOException(IOExceptionEvent arg0) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void processRequest(RequestEvent requestEvent) {
		
		this.dispatcher(requestEvent);
		
		Request request = requestEvent.getRequest();
		System.out.println("Request received: " + request.getMethod());
		System.out.println(request);
	
		}
		
		
		
	
	
	
	
	private String getContentType(Request request )
	{
		
	//	String requestteststring = "blablakksj...sdjjshhs....Content-Type: Ergebnis..Contentsdjdjjjjis...dkks";
		String contentType;
		String requestString;
		String[] headers1;
		String[] headers2;
		requestString = request.toString();
	//	requestString = requestteststring;
		 if(requestString.contains("Content-Type"))
		 {
			 headers1 = requestString.split("Content-Type: ");
			 contentType = headers1[1];
			 headers2 = contentType.split("Date");
			 contentType=headers2[0];
			 contentType = contentType.substring(0, contentType.length()-2);
			 return contentType;
		 } else return "no Content-Type found";
	}

	@Override
	public void processResponse(ResponseEvent responseEvent) {
		
		Response response = responseEvent.getResponse();
		ClientTransaction tid = responseEvent.getClientTransaction();
		CSeqHeader cseq = (CSeqHeader) response.getHeader(CSeqHeader.NAME);
		
		// hand over the request to the smartHomeClient Object		
		//smartHomeClient.Responsereceived(response);
		// hand over the reqest to the smartHomeClient Object
		//mediaServerClient.Responsereceived(response);
		 
		// answer with ACK to reveived 200OK
		if(response.getStatusCode() == Response.OK )
		{
			
		System.out.println(response);
			
			/*
			System.out.println("200 OK received - Action: Send ACK");
			dialog = tid.getDialog();
			// Senden einer ACK-Nachricht
			try {
				Request ackRequest;
				ackRequest = dialog.createAck(cseq.getSeqNumber());
				dialog.sendAck(ackRequest);
				}
			catch (InvalidArgumentException | SipException e) {
				System.out.println("Error sending ACK");
				e.printStackTrace();
				}*/
		}
		else
		{
			System.out.println("Response: " +response.getStatusCode() +" received - Action: Drop response");
			// log event
			
		} 

    }
	
	
	

	@Override
	public void processTimeout(TimeoutEvent arg0) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void processTransactionTerminated(TransactionTerminatedEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	
	

	
}
