package applet;


import javacard.framework.*;




public class TheApplet extends Applet {


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
	final static short SW_VERIFICATION_FAILED = (short)0x6300;
	final static short SW_PIN_VERIFICATION_REQUIRED = (short)0x6301;
	
	byte[] name;
	byte[] file;
	OwnerPIN pinWrite;
	OwnerPIN pinRead;
	short fileSize;
	short ptrReadFile;
	boolean pinSecurity;


	protected TheApplet() {
		this.name = new byte[0xff];
		this.fileSize=0;
		this.ptrReadFile=0;
		byte[] pincodeWrite = {(byte)0x30,(byte)0x30,(byte)0x30,(byte)0x30}; // PIN code "0000"
		pinWrite = new OwnerPIN((byte)3,(byte)8);  				// 3 tries 8=Max Size
		pinWrite.update(pincodeWrite,(short)0,(byte)4); 				// from pincode, offset 0, length 4
		
		byte[] pincodeRead = {(byte)0x31,(byte)0x31,(byte)0x31,(byte)0x31}; // PIN code "1111"
		pinRead = new OwnerPIN((byte)3,(byte)8);  				// 3 tries 8=Max Size
		pinRead.update(pincodeRead,(short)0,(byte)4); 				// from pincode, offset 0, length 4
		
		this.pinSecurity = true;
		this.file = new byte[8000];
		this.register();
		}


	public static void install(byte[] bArray, short bOffset, byte bLength) throws ISOException {
		new TheApplet();
	} 

	void verify( APDU apdu, OwnerPIN ownerpin ){
		apdu.setIncomingAndReceive();
		byte[] buffer = apdu.getBuffer();
		if (!ownerpin.check(buffer, (byte)5, buffer[(byte)4]))
			ISOException.throwIt( SW_VERIFICATION_FAILED);
	}

	public boolean select() {
		if ( pinWrite.getTriesRemaining() == 0 )
		return false;
		if ( pinRead.getTriesRemaining() == 0 )
		return false;
		return true;
	} 


	public void deselect() {
		pinWrite.reset();
		pinRead.reset();
	}


	public void process(APDU apdu) throws ISOException {
		if( selectingApplet() == true )
			return;

		byte[] buffer = apdu.getBuffer();

		switch( buffer[1] ) 	{
			case UPDATECARDKEY: updateCardKey( apdu ); break;
			case UNCIPHERFILEBYCARD: uncipherFileByCard( apdu ); break;
			case CIPHERFILEBYCARD: cipherFileByCard( apdu ); break;
			case CIPHERANDUNCIPHERNAMEBYCARD: cipherAndUncipherNameByCard( apdu ); break;
			case READFILEFROMCARD: readFileFromCard( apdu ); break;
			case WRITEFILETOCARD: writeFileToCard( apdu ); break;
			case UPDATEWRITEPIN: updateWritePIN( apdu ); break;
			case UPDATEREADPIN: updateReadPIN( apdu ); break;
			case DISPLAYPINSECURITY: displayPINSecurity( apdu ); break;
			case DESACTIVATEACTIVATEPINSECURITY: desactivateActivatePINSecurity( apdu ); break;
			case ENTERREADPIN: enterReadPIN( apdu ); break;
			case ENTERWRITEPIN: enterWritePIN( apdu ); break;
			case READNAMEFROMCARD: readNameFromCard( apdu ); break;
			case WRITENAMETOCARD: writeNameToCard( apdu ); break;
			default: ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
		}
	}


	void updateCardKey( APDU apdu ) {
	}


	void uncipherFileByCard( APDU apdu ) {
	}


	void cipherFileByCard( APDU apdu ) {
	
	
	}


	void cipherAndUncipherNameByCard( APDU apdu ) {
			if((pinSecurity==true && pinWrite.isValidated()) || pinSecurity==false){
		apdu.setIncomingAndReceive();
		byte[] buffer = apdu.getBuffer();
		Util.arrayCopy(buffer,(short)4,name,(short)0,(short)((byte)1+buffer[4]));}
		else 
			ISOException.throwIt(SW_PIN_VERIFICATION_REQUIRED);
		
	}


	void readFileFromCard( APDU apdu ) {

		byte[] buffer = apdu.getBuffer();
		short NBapdu = (short)(fileSize/127);
		byte reste = (byte)(fileSize%127);
		if(buffer[2] ==0  && buffer[3]==0)		
		ptrReadFile=0;
		 if(NBapdu==ptrReadFile){
		Util.arrayCopy(file, (short)(ptrReadFile*127), buffer, (short)0, (short)reste);			
		apdu.setOutgoingAndSend( (short)0, (short)reste );		
		ptrReadFile=-1;
		 }
		 else if(ptrReadFile==-1){
		 ISOException.throwIt(SW_EOF);
		 }
		 else{
		Util.arrayCopy(file, (short)(ptrReadFile*127), buffer, (short)0, (short)127);		
		apdu.setOutgoingAndSend( (short)0, (short)127 );
		ptrReadFile++;
		}
	/*	
		for(short i=0;i<(short)((fileSize/127)+1);i++){
			 if(NBapdu==i){
			Util.arrayCopy(file, (short)(i*127), buffer, (short)0, (short)(127*i+reste));		
			apdu.setOutgoingAndSend( (short)0, (short)reste );
			}
			else{
			Util.arrayCopy(file, (short)(i*127), buffer, (short)0, (short)(127*(i+1)));		
			apdu.setOutgoingAndSend( (short)0, (short)127 );
			}
		}*/
	}


	void writeFileToCard( APDU apdu ) {
		apdu.setIncomingAndReceive();
		byte[] buffer = apdu.getBuffer();
		if(buffer[2] ==0  && buffer[3]==0){
		fileSize=0;
		}
		Util.arrayCopy(buffer,(short)5,file,fileSize,(short)buffer[4]);
		fileSize+=buffer[4];
	}


	void updateWritePIN( APDU apdu ) {
		if((pinSecurity==true && pinWrite.isValidated()) || pinSecurity==false){
		apdu.setIncomingAndReceive();
		byte[] buffer = apdu.getBuffer();
		pinWrite.update(buffer,(short)5,(byte)4);}
		else 
			ISOException.throwIt(SW_PIN_VERIFICATION_REQUIRED);
	}


	void updateReadPIN( APDU apdu ) {
		if((pinSecurity==true && pinRead.isValidated()) || pinSecurity==false){
		apdu.setIncomingAndReceive();
		byte[] buffer = apdu.getBuffer();
		pinRead.update(buffer,(short)5,(byte)4);}
		else 
			ISOException.throwIt(SW_PIN_VERIFICATION_REQUIRED);
	}


	void displayPINSecurity( APDU apdu ) {
		byte[] buffer = apdu.getBuffer();
		buffer[0]= buffer[1] = (pinSecurity?(byte)0x01:(byte)0x00);
		apdu.setOutgoingAndSend( (short)0,(short)2);
	}


	void desactivateActivatePINSecurity( APDU apdu ) {
		apdu.setIncomingAndReceive();
		byte[] buffer = apdu.getBuffer();
		pinSecurity=(buffer[5]!=0x30);
	}


	void enterReadPIN( APDU apdu ) {
		verify( apdu, pinRead );
	}


	void enterWritePIN( APDU apdu ) {
		verify( apdu, pinWrite );
	}


	void readNameFromCard( APDU apdu ) {
		if((pinSecurity==true && pinRead.isValidated()) || pinSecurity==false){
		byte[] buffer = apdu.getBuffer();
		Util.arrayCopy(name, (short)1, buffer, (short)0, name[0]);
		apdu.setOutgoingAndSend( (short)0, name[0] );}
		else 
			ISOException.throwIt(SW_PIN_VERIFICATION_REQUIRED);
	}


	void writeNameToCard( APDU apdu ){ 
		if((pinSecurity==true && pinWrite.isValidated()) || pinSecurity==false){
		apdu.setIncomingAndReceive();
		byte[] buffer = apdu.getBuffer();
		Util.arrayCopy(buffer,(short)4,name,(short)0,(short)((byte)1+buffer[4]));}
		else 
			ISOException.throwIt(SW_PIN_VERIFICATION_REQUIRED);
	}


}
