package client;

import java.io.*;
import java.net.*;
import java.util.*;
import opencard.core.service.*;
import opencard.core.terminal.*;
import opencard.core.util.*;
import opencard.opt.util.*;




public class TheClient extends Thread{

	private PassThruCardService servClient = null;
	boolean DISPLAY = true;

	static final byte CLA					= (byte)0x00;
	static final byte P1					= (byte)0x00;
	static final byte P2					= (byte)0x00;
	static final byte UPDATECARDKEY				= (byte)0x14;
	static final byte UNCIPHERFILEBYCARD			= (byte)0x13;
	static final byte CIPHERFILEBYCARD			= (byte)0x12;
	static final byte CIPHERANDUNCIPHERNAMEBYCARD		= (byte)0x11;
	static final byte READFILEFROMCARD			= (byte)0x10;
	static final byte WRITEFILETOCARD			= (byte)0x09;
	static final byte UPDATEWRITEPIN			= (byte)0x08;
	static final byte UPDATEREADPIN				= (byte)0x07;
	static final byte DISPLAYPINSECURITY			= (byte)0x06;
	static final byte DESACTIVATEACTIVATEPINSECURITY	= (byte)0x05;
	static final byte ENTERREADPIN				= (byte)0x04;
	static final byte ENTERWRITEPIN				= (byte)0x03;
	static final byte READNAMEFROMCARD			= (byte)0x02;
	static final byte WRITENAMETOCARD			= (byte)0x01;
	private final static byte CLA_TEST							= (byte)0x90;
	private final static byte INS_TESTDES_ECB_NOPAD_ENC       	= (byte)0x28;
	private final static byte INS_TESTDES_ECB_NOPAD_DEC       	= (byte)0x29;
	private final static byte INS_DES_ECB_NOPAD_ENC           	= (byte)0x20;
	private final static byte INS_DES_ECB_NOPAD_DEC           	= (byte)0x21;
	private final static byte P1_EMPTY = (byte)0x00;
	private final static byte P2_EMPTY = (byte)0x00;

	static final int port = 6010;
	static final String broadcast_ip = "255.255.255.255";
	String nickname = null;

	public TheClient() {
		try {
			SmartCard.start();
			System.out.print( "Smartcard inserted?... " ); 

			CardRequest cr = new CardRequest (CardRequest.ANYCARD,null,null); 

			SmartCard sm = SmartCard.waitForCard (cr);

			if (sm != null) {
				System.out.println ("got a SmartCard object!\n");
			} else
				System.out.println( "did not get a SmartCard object!\n" );

			this.initNewCard( sm ); 

			SmartCard.shutdown();

		} catch( Exception e ) {
			System.out.println( "TheClient error: " + e.getMessage() );
		}
		java.lang.System.exit(0) ;
	}

	private ResponseAPDU sendAPDU(CommandAPDU cmd) {
		return sendAPDU(cmd, true);
	}

	private ResponseAPDU sendAPDU( CommandAPDU cmd, boolean display ) {
		ResponseAPDU result = null;
		try {
			result = this.servClient.sendCommandAPDU( cmd );
			if(display)
				displayAPDU(cmd, result);
		} catch( Exception e ) {
			System.out.println( "Exception caught in sendAPDU: " + e.getMessage() );
			java.lang.System.exit( -1 );
		}
		return result;
	}


	/************************************************
	* *********** BEGINNING OF TOOLS ***************
	* **********************************************/


	private String apdu2string( APDU apdu ) {
		return removeCR( HexString.hexify( apdu.getBytes() ) );
	}


	public void displayAPDU( APDU apdu ) {
		System.out.println( removeCR( HexString.hexify( apdu.getBytes() ) ) + "\n" );
	}


	public void displayAPDU( CommandAPDU termCmd, ResponseAPDU cardResp ) {
		System.out.println( "--> Term: " + removeCR( HexString.hexify( termCmd.getBytes() ) ) );
		System.out.println( "<-- Card: " + removeCR( HexString.hexify( cardResp.getBytes() ) ) );
	}


	private String removeCR( String string ) {
		return string.replace( '\n', ' ' );
	}


	/******************************************
	* *********** END OF TOOLS ***************
	* ****************************************/


	private boolean selectApplet() {
		boolean cardOk = false;
		try {
			CommandAPDU cmd = new CommandAPDU( new byte[] {
				(byte)0x00, (byte)0xA4, (byte)0x04, (byte)0x00, (byte)0x0A,
					(byte)0xA0, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x62, 
					(byte)0x03, (byte)0x01, (byte)0x0C, (byte)0x06, (byte)0x01
			} );
			ResponseAPDU resp = this.sendAPDU( cmd );
			if( this.apdu2string( resp ).equals( "90 00" ) )
				cardOk = true;
		} catch(Exception e) {
			System.out.println( "Exception caught in selectApplet: " + e.getMessage() );
			java.lang.System.exit( -1 );
		}
		return cardOk;
	}


	private void initNewCard( SmartCard card ) {
		if( card != null )
			System.out.println( "Smartcard inserted\n" );
		else {
			System.out.println( "Did not get a smartcard" );
			System.exit( -1 );
		}

		System.out.println( "ATR: " + HexString.hexify( card.getCardID().getATR() ) + "\n");


		try {
			this.servClient = (PassThruCardService)card.getCardService( PassThruCardService.class, true );
		} catch( Exception e ) {
			System.out.println( e.getMessage() );
		}

		System.out.println("Applet selecting...");
		if( !this.selectApplet() ) {
			System.out.println( "Wrong card, no applet to select!\n" );
			System.exit( 1 );
			return;
		} else 
			System.out.println( "Applet selected" );

		MultiChatUDP();
	}


	String readKeyboard() {
		String result = null;

		try {
			BufferedReader input = new BufferedReader( new InputStreamReader( System.in ) );
			result = input.readLine();
		} catch( Exception e ) {}

		return result;
	}


	public static void main( String[] args ) throws InterruptedException {
		new TheClient();
	}


	public void MultiChatUDP() {
		if( nickname == null ) {
			System.out.print( "Enter your nickname: " );
			nickname = readKeyboard();
		}
		start();
		sendMessagesLoop();
	}

	public void run() {
		receiveMessagesLoop();
	}

	byte[] uncipherAndUnpadding(byte[] cipher_msg){	

		int count=0;
		byte [] tmp=cipherGeneric(INS_DES_ECB_NOPAD_DEC, cipher_msg);
		while(tmp[count]!=0)
			count++;
		for(;count<32;count++)
			tmp[count]=(byte)0x20; //space char		
		return	tmp;
	}

	void receiveMessagesLoop() {
		System.out.println("Received messages from all subnetwork will be displayed.");
		while(true) try {
			byte[] message = new byte[32];
			byte[] plain ;	

			DatagramSocket socket = new DatagramSocket(port);
			DatagramPacket packet = new DatagramPacket(message,message.length);
			socket.receive(packet);
			plain=uncipherAndUnpadding(message);
			String s = new String(plain);
			System.out.println( "<" + packet.getAddress().getHostName() + "> " + s);
		} catch( Exception e ) {}
	}


	byte[] cipherAndpadding(byte[]plain_msg){
		int i=0;
		int MAX_SIZE=32;
		int length=plain_msg.length;
		byte[] buf= new byte [MAX_SIZE];
		byte[] ret_buff = new byte[length+8-length%8];
		while(i+MAX_SIZE<length && length>=MAX_SIZE)	{
			buf=Arrays.copyOfRange(plain_msg, i, i+MAX_SIZE);			
			buf = cipherGeneric(INS_DES_ECB_NOPAD_ENC, buf);	
			System.arraycopy(buf, 0, ret_buff, i, buf.length);
			i+=MAX_SIZE;
		}
		if(i<length){
			int size=length-i;
			int padding=8-size%8;
			//byte[] tmp_padd= new byte [size+padding];
			byte[] buff_space_char=new byte[32];
			//for(int j=0;j>=padding;j++)
			//	buff_space_char[j]=(byte)0x00; //space char
			System.arraycopy(plain_msg, i, ret_buff, i, size); 		//le reste de data
			//System.arraycopy(buff_space_char, 0, ret_buff, i+size, 32-padding);// le reste padding				
		}		

		return cipherGeneric(INS_DES_ECB_NOPAD_ENC, ret_buff);

	}

	void sendMessagesLoop() {
		System.out.println("Typed messages will be sent to all subnetwork.");
		while(true) try {
			String msg = "[" + nickname + "] " + readKeyboard();
			byte[] message = msg.getBytes();
			byte[] cipher_msg = cipherAndpadding( message);
			InetAddress address = InetAddress.getByName(broadcast_ip);
			DatagramPacket packet = new DatagramPacket(cipher_msg, cipher_msg.length, address, port);
			DatagramSocket socket = new DatagramSocket();
			socket.send(packet);
		} catch( Exception e ) {}
	}

	private byte[] cipherGeneric( byte typeINS, byte[] challenge ) {
		byte[] result = new byte[challenge.length];
		byte[] header = {CLA_TEST,typeINS,P1_EMPTY,P1_EMPTY,(byte)challenge.length};		
		byte[] apdu = new byte[header.length + result.length + 1];	
		System.arraycopy(header, 0, apdu, 0, header.length);
		System.arraycopy(challenge, 0, apdu, header.length, result.length);		
		apdu[apdu.length-1]=(byte)challenge.length;
		CommandAPDU cmd = new CommandAPDU( apdu );
		ResponseAPDU resp = this.sendAPDU( cmd, DISPLAY );
		byte[] bytes = resp.getBytes();
		System.arraycopy(bytes, 0, result,0 , bytes.length-2);
		return result;
	}

}
