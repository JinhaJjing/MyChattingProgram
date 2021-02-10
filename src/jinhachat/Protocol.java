package jinhachat;

import java.io.Serializable;

public class Protocol implements Serializable {
    //프로토콜 타입에 관한 변수
    public static final int PT_UNDEFINED = -1;   //프로토콜이 지정되어 있지 않을 경우에
    public static final int PT_EXIT = 0;
    public static final int PT_REQ_LOGIN = 1;   //로그인요청
    public static final int PT_RES_LOGIN = 2;   //인증요청
    public static final int PT_LOGIN_RESULT = 3;  //인증결과
    public static final int LEN_LOGIN_ID = 20;   //ID길이
    public static final int LEN_LOGIN_CHATROOM = 20; //CHATROOM길이
    public static final int LEN_LOGIN_RESULT = 2;  //로그인인증값 길이
    public static final int LEN_PROTOCOL_TYPE = 1;  //프로토콜타입 길이
    public static final int LEN_MAX = 1000;    //최대 데이터 길이

    protected int protocolType;

    private byte[] packet;   //프로토콜과 데이터의 저장공간이 되는 바이트배열

    public Protocol() {
        this(PT_UNDEFINED);
    }

    public Protocol(int protocolType) {
        this.protocolType = protocolType;
        //어떤 상수를 생성자에 넣어 Protocol 클래스를 생성하느냐에 따라서 바이트배열 packet 의 length 가 결정된다.
        getPacket(protocolType);
    }

    public byte[] getPacket(int protocolType){

        if(packet == null){
            switch(protocolType){
                case PT_REQ_LOGIN : packet = new byte[LEN_PROTOCOL_TYPE]; break;
                case PT_RES_LOGIN : packet = new byte[LEN_PROTOCOL_TYPE + LEN_LOGIN_ID + LEN_LOGIN_CHATROOM]; break;
                case PT_UNDEFINED : packet = new byte[LEN_MAX]; break;
                case PT_LOGIN_RESULT : packet = new byte[LEN_PROTOCOL_TYPE + LEN_LOGIN_RESULT]; break;
                case PT_EXIT : packet = new byte[LEN_PROTOCOL_TYPE]; break;
            }
        }

        packet[0] = (byte)protocolType;   //packet 바이트배열의 첫번째 방에 프로토콜타입 상수를 셋팅해 놓는다.
        return packet;
    }

    //로그인후 성공 or 실패의 결과값을 프로토콜로부터 추출하여 문자열로 리턴
    public String getLoginResult(){
        //String의 다음 생성자를 사용 : String(byte[] bytes, int offset, int length)
        return new String(packet, LEN_PROTOCOL_TYPE, LEN_LOGIN_RESULT).trim();
    }


    //String ok를 byte[]로 만들어 packet의 프로토콜 타입 바로 뒤에 추가한다.
    public void setLoginResult(String ok){
        System.arraycopy(ok.trim().getBytes(), 0, packet, LEN_PROTOCOL_TYPE, ok.trim().getBytes().length);
    }


    public void setProtocolType(int protocolType){
        this.protocolType = protocolType;
    }


    public int getProtocolType(){
        return protocolType;
    }


    public byte[] getPacket(){
        return packet;
    }


    //Default 생성자로 생성한 후 Protocol 클래스의 packet 데이터를 바꾸기 위한 메서드
    public void setPacket(int pt, byte[] buf){
        packet = null;
        packet = getPacket(pt);
        protocolType = pt;
        System.arraycopy(buf, 0, packet, 0, packet.length);
    }


    public String getId(){
        return new String(packet, LEN_PROTOCOL_TYPE, LEN_LOGIN_ID).trim();
    }


    //String ID를 byte[]로 만들어 byte[] packet의 프로토콜 타입 바로 뒷부분에 추가한다.
    public void setId(String id){
        System.arraycopy(id.trim().getBytes(), 0, packet, LEN_PROTOCOL_TYPE, id.trim().getBytes().length);
    }


    public String getChatroom(){
        return new String(packet, LEN_PROTOCOL_TYPE + LEN_LOGIN_ID, LEN_LOGIN_CHATROOM).trim();
    }


    //byte[] 에서 로그인 아이디 뒷부분에 채팅방 이름이 들어간다.
    public void setChatroom(String chatroom){
        System.arraycopy(chatroom.trim().getBytes(), 0, packet, LEN_PROTOCOL_TYPE+LEN_LOGIN_ID, chatroom.trim().getBytes().length);
        packet[LEN_PROTOCOL_TYPE + LEN_LOGIN_ID + chatroom.trim().getBytes().length] = '\0';
    }

}