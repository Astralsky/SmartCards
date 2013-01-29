package client;

import java.io.*;
import java.util.*;
import opencard.core.service.*;
import opencard.core.terminal.*;
import opencard.core.util.*;
import opencard.opt.util.*;




public class TheClient {



	private final static byte CLA_TEST							= (byte)0x90;
	private final static byte INS_TESTDES_ECB_NOPAD_ENC       	= (byte)0x28;
	private final static byte INS_TESTDES_ECB_NOPAD_DEC       	= (byte)0x29;
	private final static byte INS_DES_ECB_NOPAD_ENC           	= (byte)0x20;
	private final static byte INS_DES_ECB_NOPAD_DEC           	= (byte)0x21;
	private final static byte P1_EMPTY = (byte)0x00;
	private final static byte P2_EMPTY = (byte)0x00;
	private final static int MAX_SIZE = 248;


	private PassThruCardService servClient = null;
	boolean loop = true;
	boolean DISPLAY = false;


	public static void main( String[] args ) throws InterruptedException {
		new TheClient();
	}


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
			System.out.println( "Applet selected\n" );

		foo();
	}


	private void testDES_ECB_NOPAD( boolean displayAPDUs ) { 
		testCryptoGeneric(INS_TESTDES_ECB_NOPAD_ENC);
		testCryptoGeneric(INS_TESTDES_ECB_NOPAD_DEC);
	}


	private void testCryptoGeneric( byte typeINS ) {
		byte[] t = new byte[4];

		t[0] = CLA_TEST;
		t[1] = typeINS;
		t[2] = P1_EMPTY;
		t[3] = P2_EMPTY;

		this.sendAPDU(new CommandAPDU( t ));
	} 


	private byte[] cipherDES_ECB_NOPAD( byte[] challenge, boolean display ) {
		return cipherGeneric( INS_DES_ECB_NOPAD_ENC, challenge );
	} 


	private byte[] uncipherDES_ECB_NOPAD( byte[] challenge, boolean display ) {
		return cipherGeneric( INS_DES_ECB_NOPAD_DEC, challenge );
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


	private void foo() {
		mainLoop();

	}

	byte[] check_pad(byte[] buf) {
		//System.out.println(new String(buf));
		int count = buf.length-1;
		while(buf[count]==0)
			count--;		  

		byte[] ret;
		if(buf[count]==buf.length-1-count){ //remove padding
			ret=new byte[count];
			System.arraycopy(buf, 0, ret, 0, count);		
		}
		else{ // remove 8 bytes of padding (no padding)
			ret=new byte[buf.length-8];
			System.arraycopy(buf, 0, ret, 0, buf.length-8);
		}
		return ret;
	}

	void write_file(String path,byte[] buffer){
		try{
			FileOutputStream fos = new FileOutputStream(path);
			fos.write(buffer);
			fos.close();
		}catch (Exception e){
			System.out.println(e.toString());
		}	
	}

	void uncipherFileByCard() {

		InputStream is = null;
		DataInputStream dis = null;
		try{	        
			is = new FileInputStream("cipher_test");  	// create file input stream        
			dis = new DataInputStream(is); 	//create new data input stream 		
			int length = dis.available(); 		// available stream to be read         
			byte[] buf = new byte[length]; 	// create buffer
			byte[] buffer = new byte[length];   //create buffer save cipher file

			dis.readFully(buf); // read the full data into the buffer

			byte[] tmp= new byte [MAX_SIZE];
			int i=0;
			byte[] resp=new byte[MAX_SIZE];
			while(i+MAX_SIZE<=length)	{
				tmp=Arrays.copyOfRange(buf, i, i+MAX_SIZE);			
				resp = cipherGeneric(INS_DES_ECB_NOPAD_DEC, tmp);
				System.arraycopy(resp, 0, buffer, i, resp.length);
				i+=MAX_SIZE;
			}
			if(i+MAX_SIZE>length){
				tmp=Arrays.copyOfRange(buf, i, length);				
				resp = cipherGeneric(INS_DES_ECB_NOPAD_DEC, tmp);
				System.arraycopy(resp, 0, buffer, i, resp.length);			
				write_file("uncipher_test",check_pad(buffer));
			}			
		} catch(Exception e){
			System.out.println( e.toString() );
		}

	}

	void cipherFileByCard(){
		System.out.println("Entrer le chemin d'un fichier");
		String file = readKeyboard();
		InputStream is = null;
		DataInputStream dis = null;
		try{	        
			is = new FileInputStream(file);  	// create file input stream        
			dis = new DataInputStream(is); 		//create new data input stream  
			long startTime = System.currentTimeMillis();			
			int length = dis.available(); 		// available stream to be read         
			byte[] buf = new byte[length]; 		// create buffer
			byte[] buffer = new byte[(length%8==1)?length+16-length%8 : length+8-length%8];   //create buffer save cipher file

			dis.readFully(buf); // read the full data into the buffer
			byte[] tmp= new byte [MAX_SIZE];
			int i=0;
			byte[] resp=new byte[MAX_SIZE];
			while(i+MAX_SIZE<length && length>=MAX_SIZE)	{
				tmp=Arrays.copyOfRange(buf, i, i+MAX_SIZE);			
				resp = cipherGeneric(INS_DES_ECB_NOPAD_ENC, tmp);	
				System.arraycopy(resp, 0, buffer, i, resp.length);
				i+=MAX_SIZE;
			}
			if(i==length){//force add padding
				byte[] buff_zero=new byte[8];
				buff_zero[0]=(byte)7;	
				resp = cipherGeneric(INS_DES_ECB_NOPAD_ENC, buff_zero);			
				System.arraycopy(resp, 0, buffer, length, (byte)8);
			}
			if(i<length){

				int size=length-i;
				int padding=8-size%8;
				byte[] tmp_padd= new byte [size+padding];
				byte[] buff_zero=new byte[padding];
				//for padding
				buff_zero[0]=(byte)(  (padding==1) ? padding-1+8 : padding-1);	

				System.arraycopy(buf, i, tmp_padd, 0, size); 		//le reste de data
				System.arraycopy(buff_zero, 0, tmp_padd, size, padding);// le reste padding

				resp = cipherGeneric(INS_DES_ECB_NOPAD_ENC, tmp_padd);				
				System.arraycopy(resp, 0, buffer, i, resp.length); //bug init buffer size				
				if(padding==1){				
					byte[] zero=new byte[8]; //padding 8 zero
					resp = cipherGeneric(INS_DES_ECB_NOPAD_ENC, zero);
					System.arraycopy(resp, 0, buffer, i+size+padding, resp.length);
				}
			}
			write_file("cipher_test",buffer);
			long time = (System.currentTimeMillis() - startTime) / 1000;
			System.out.println("" + time + " s");
		} catch(Exception e){
			System.out.println( e.toString() );
		}

	}

	void exit() {
		loop = false;
	}


	void runAction( int choice ) {
		switch( choice ) {
		case 2: uncipherFileByCard(); break;
		case 1: cipherFileByCard(); break;
		case 0: exit(); break;
		default: System.out.println( "unknown choice!" );
		}
	}

	String readKeyboard() {
		String result = null;

		try {
			BufferedReader input = new BufferedReader( new InputStreamReader( System.in ) );
			result = input.readLine();
		} catch( Exception e ) {}

		return result;
	}

	int readMenuChoice() {
		int result = 0;
		try {
			String choice = readKeyboard();
			result = Integer.parseInt( choice );
		} catch( Exception e ) {}
		System.out.println( "" );
		return result;
	}
	void printMenu() {
		System.out.println( "" );
		System.out.println( "2: Uncipher a File by the card" );
		System.out.println( "1: Cipher a file by the card" );
		System.out.println( "0: exit" );
		System.out.print( "--> " );
	}

	void mainLoop() {
		while( loop ) {
			printMenu();
			int choice = readMenuChoice();
			runAction( choice );
		}
	}
}