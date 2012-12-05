package client;

import java.io.*;
import java.util.*;
import opencard.core.service.*;
import opencard.core.terminal.*;
import opencard.core.util.*;
import opencard.opt.util.*;




public class TheClient {

	private PassThruCardService servClient = null;
	boolean DISPLAY = true;
	boolean loop = true;

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
	final static short SW_EOF					= (short)0x8800;
	//String msg = "";
	


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

		mainLoop();
	}


	void updateCardKey() {
	}


	void uncipherFileByCard() {
	}


	void cipherFileByCard() {
	}


	void cipherAndUncipherNameByCard() {
		System.out.println("Entrer un nom");
		String name = readKeyboard();
		byte[] name_b = name.getBytes();
		
		byte[] header = {CLA, CIPHERANDUNCIPHERNAMEBYCARD, P1, P2, (byte)name_b.length };
		byte[] apdu = new byte[header.length + name_b.length];
		
		System.arraycopy(header, 0, apdu, 0, header.length);
		System.arraycopy(name_b, 0, apdu, header.length, name_b.length);

		CommandAPDU cmd = new CommandAPDU( apdu );
		displayAPDU(cmd);
		ResponseAPDU resp = this.sendAPDU( cmd, DISPLAY );
	}


	void readFileFromCard() {
	byte[] file=new byte[8000];
	byte tmp=0;
	String strFile= "";
	short count=0;
			
		while(true) {   
		byte[] cmd_ = {CLA,READFILEFROMCARD,tmp,P2,(byte)0x84};			
		CommandAPDU cmd = new CommandAPDU( cmd_ );			
        ResponseAPDU resp = this.sendAPDU( cmd, DISPLAY );
		if( removeCR( HexString.hexify( resp.getBytes() ) ).equals( "88 00" ) )
		break;
	    byte[] bytes = resp.getBytes();
	    String msg = "";
		count+=(short)(bytes.length-2);
		//System.arraycopy(bytes, 0, file, count, bytes.length-2);
	    for(int i=0; i<bytes.length-2;i++)
		    msg += new StringBuffer("").append((char)bytes[i]);
	    //System.out.println("Name= "+msg);
		strFile+=msg;
		tmp++;
		}
		try{
		FileOutputStream fos = new FileOutputStream("output_file");
		fos.write(strFile.getBytes());
		fos.close();
		System.out.println("File= "+strFile);
		}
				catch (Exception e){
			System.out.println(e.toString());
		}
	}


	void writeFileToCard() {
	System.out.println("Entrer le chemin d'un fichier");
	String file = readKeyboard();
	    InputStream is = null;
		DataInputStream dis = null;
		try{	        
         is = new FileInputStream(file);  // create file input stream        
         dis = new DataInputStream(is); //create new data input stream              
         int length = dis.available(); // available stream to be read         
         byte[] buf = new byte[length]; // create buffer
		 int NBapdu = (buf.length)/127;
		 int reste = (buf.length)%127;
		 
         // read the full data into the buffer
         dis.readFully(buf);
		 byte p1 =0;
		 byte p2 =0;
		 
		 for(int i=0;i<(buf.length/127)+1;i++){
					
					if(NBapdu==i){
					byte[] tmp= new byte[reste];
					tmp=Arrays.copyOfRange(buf, i*127, 127*i+reste);
					byte[] header = {CLA, WRITEFILETOCARD, p1, p2, (byte)tmp.length };
					byte[] apdu = new byte[header.length + tmp.length];
					
					System.arraycopy(header, 0, apdu, 0, header.length);
					System.arraycopy(tmp, 0, apdu, header.length, tmp.length);

					CommandAPDU cmd = new CommandAPDU( apdu );
					//displayAPDU(cmd);
					ResponseAPDU resp = this.sendAPDU( cmd, DISPLAY );
					}
					else{
					byte[] tmp= new byte [127];
					tmp=Arrays.copyOfRange(buf, i*127, 127*(1+i));

					byte[] header = {CLA, WRITEFILETOCARD, p1, p2, (byte)tmp.length };
					byte[] apdu = new byte[header.length + tmp.length];
					
					System.arraycopy(header, 0, apdu, 0, header.length);
					System.arraycopy(tmp, 0, apdu, header.length, tmp.length);

					CommandAPDU cmd = new CommandAPDU( apdu );
					displayAPDU(cmd);
					ResponseAPDU resp = this.sendAPDU( cmd, DISPLAY );
				 
					p1+=1;
					 if(i<127){
						p2+=1;
						p1=0;
					 }
					}			
		 } 

		}		
		catch (Exception e){
			System.out.println(e.toString());
		}

	}


	void updateWritePIN() {
		System.out.println("Entrer le nouveau code pin pour Write");
		String name = readKeyboard();
		byte[] name_b = name.getBytes();
		
		byte[] header = {CLA, UPDATEWRITEPIN, P1, P2, (byte)name_b.length };
		byte[] apdu = new byte[header.length + name_b.length];
		
		System.arraycopy(header, 0, apdu, 0, header.length);
		System.arraycopy(name_b, 0, apdu, header.length, name_b.length);

		CommandAPDU cmd = new CommandAPDU( apdu );
		displayAPDU(cmd);
		ResponseAPDU resp = this.sendAPDU( cmd, DISPLAY );
	}


	void updateReadPIN() {
		System.out.println("Entrer le nouveau code pin pour Read");
		String name = readKeyboard();
		byte[] name_b = name.getBytes();
		
		byte[] header = {CLA, UPDATEREADPIN, P1, P2, (byte)name_b.length };
		byte[] apdu = new byte[header.length + name_b.length];
		
		System.arraycopy(header, 0, apdu, 0, header.length);
		System.arraycopy(name_b, 0, apdu, header.length, name_b.length);

		CommandAPDU cmd = new CommandAPDU( apdu );
		displayAPDU(cmd);
		ResponseAPDU resp = this.sendAPDU( cmd, DISPLAY );
	}


	void displayPINSecurity() {
		byte[] cmd_ = {CLA,DISPLAYPINSECURITY,P1,P2,(byte)0x02};
            CommandAPDU cmd = new CommandAPDU( cmd_ );
            ResponseAPDU resp = this.sendAPDU( cmd, DISPLAY );
	    byte[] bytes = resp.getBytes();
		if(bytes[1]==1)
			System.out.println("pinSecurity est active");
		else
			System.out.println("pinSecurity est desactive");
		}


	void desactivateActivatePINSecurity() {
		System.out.println("Taper 1 pour activer et 0 pour desactiver ");
		String pin = readKeyboard();
		byte[] pin_b = pin.getBytes();
		
		byte[] header = {CLA, DESACTIVATEACTIVATEPINSECURITY, P1, P2, (byte)pin_b.length };
		byte[] apdu = new byte[header.length + pin_b.length];
		
		System.arraycopy(header, 0, apdu, 0, header.length);
		System.arraycopy(pin_b, 0, apdu, header.length, pin_b.length);

		CommandAPDU cmd = new CommandAPDU( apdu );
		displayAPDU(cmd);
		ResponseAPDU resp = this.sendAPDU( cmd, DISPLAY );
	}


	void enterReadPIN() {
		System.out.println("Entrer un pin ");
		String pin = readKeyboard();
		byte[] pin_b = pin.getBytes();
		
		byte[] header = {CLA, ENTERREADPIN, P1, P2, (byte)pin_b.length };
		byte[] apdu = new byte[header.length + pin_b.length];
		
		System.arraycopy(header, 0, apdu, 0, header.length);
		System.arraycopy(pin_b, 0, apdu, header.length, pin_b.length);

		CommandAPDU cmd = new CommandAPDU( apdu );
		displayAPDU(cmd);
		ResponseAPDU resp = this.sendAPDU( cmd, DISPLAY );
	}


	void enterWritePIN() {
		System.out.println("Entrer un pin ");
		String pin = readKeyboard();
		byte[] pin_b = pin.getBytes();
		
		byte[] header = {CLA, ENTERWRITEPIN, P1, P2, (byte)pin_b.length };
		byte[] apdu = new byte[header.length + pin_b.length];
		
		System.arraycopy(header, 0, apdu, 0, header.length);
		System.arraycopy(pin_b, 0, apdu, header.length, pin_b.length);

		CommandAPDU cmd = new CommandAPDU( apdu );
		displayAPDU(cmd);
		ResponseAPDU resp = this.sendAPDU( cmd, DISPLAY );
	}


	void readNameFromCard() {
		byte[] cmd_ = {CLA,READNAMEFROMCARD,P1,P2,(byte)0x81};
        CommandAPDU cmd = new CommandAPDU( cmd_ );
        ResponseAPDU resp = this.sendAPDU( cmd, DISPLAY );
	    byte[] bytes = resp.getBytes();
	    String msg = "";
	    for(int i=0; i<bytes.length-2;i++)
		    msg += new StringBuffer("").append((char)bytes[i]);
	    System.out.println("Name= "+msg);
	}


	void writeNameToCard() {
		System.out.println("Entrer un nom");
		String name = readKeyboard();
		byte[] name_b = name.getBytes();
		
		byte[] header = {CLA, WRITENAMETOCARD, P1, P2, (byte)name_b.length };
		byte[] apdu = new byte[header.length + name_b.length];
		
		System.arraycopy(header, 0, apdu, 0, header.length);
		System.arraycopy(name_b, 0, apdu, header.length, name_b.length);

		CommandAPDU cmd = new CommandAPDU( apdu );
		displayAPDU(cmd);
		ResponseAPDU resp = this.sendAPDU( cmd, DISPLAY );
	}


	void exit() {
		loop = false;
	}


	void runAction( int choice ) {
		switch( choice ) {
			case 14: updateCardKey(); break;
			case 13: uncipherFileByCard(); break;
			case 12: cipherFileByCard(); break;
			case 11: cipherAndUncipherNameByCard(); break;
			case 10: readFileFromCard(); break;
			case 9: writeFileToCard(); break;
			case 8: updateWritePIN(); break;
			case 7: updateReadPIN(); break;
			case 6: displayPINSecurity(); break;
			case 5: desactivateActivatePINSecurity(); break;
			case 4: enterReadPIN(); break;
			case 3: enterWritePIN(); break;
			case 2: readNameFromCard(); break;
			case 1: writeNameToCard(); break;
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
		System.out.println( "14: update the DES key within the card" );
		System.out.println( "13: uncipher a file by the card" );
		System.out.println( "12: cipher a file by the card" );
		System.out.println( "11: cipher and uncipher a name by the card" );
		System.out.println( "10: read a file from the card" );
		System.out.println( "9: write a file to the card" );
		System.out.println( "8: update WRITE_PIN" );
		System.out.println( "7: update READ_PIN" );
		System.out.println( "6: display PIN security status" );
		System.out.println( "5: desactivate/activate PIN security" );
		System.out.println( "4: enter READ_PIN" );
		System.out.println( "3: enter WRITE_PIN" );
		System.out.println( "2: read a name from the card" );
		System.out.println( "1: write a name to the card" );
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


	public static void main( String[] args ) throws InterruptedException {
		new TheClient();
	}


}
