package jinhachat;

import java.nio.ByteBuffer;

/*
 * 통신의 총 Byte는 16byte이다.
 * |MagicN|Type|ID Length|ChatRoom Length|RESERVED|
 */

public class ProtocolHeader {

    public enum PROTOCOL_OPT {
        REQ_LOGIN((byte) 0), //로그인 요청
        RES_LOGIN_SUCCESS((byte) 1), //로그인 성공
        RES_LOGIN_FAIL((byte) 2), //로그인 실패
        REQ_CHAT((byte) 3), //채팅 요청
        RES_CHAT_SUCCESS((byte) 4), //채팅 성공
        RES_CHAT_FAIL((byte) 5); //채팅 실패

        public static PROTOCOL_OPT valueOf(byte value) {
            for (PROTOCOL_OPT type : PROTOCOL_OPT.values()) {
                if (type.getValue() == value) return type;
            }
            return null;
        }

        private byte value;

        PROTOCOL_OPT(byte value) {
            this.value = value;
        }

        public byte getValue() {
            return value;
        }
    }

    public static final int HEADER_LENGTH = 16;
    private static final byte MAGIC_NUMBER = (byte) 0xAB;

    private byte magicNumber;
    private PROTOCOL_OPT protocolType;
    private int IDLength = 0;
    private int MSGLength = 0;

    public ProtocolHeader() {
    }

    public void parse(ByteBuffer byteBuffer) {
        setMagicNumber(byteBuffer.get());
        if (getMagicNumber() != MAGIC_NUMBER) return; //나중에 예외처리
        setProtocolType(PROTOCOL_OPT.valueOf(byteBuffer.get()));
        setIDLength(byteBuffer.getInt());
        setMSGLength(byteBuffer.getInt());

        if (byteBuffer.position() < HEADER_LENGTH) { //(수정)bytebuffer.get여러개 -> position을 옮기는 방법
            byteBuffer.position(HEADER_LENGTH);
        }
    }

    //전달할 변수들을 ByteBuffer에 저장
    public ByteBuffer packetize() { //(수정)매개변수 byteBuffer -> 새로운 버퍼 return
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);

        byteBuffer.put(getMagicNumber());
        byteBuffer.put((byte) getProtocolType().ordinal());
        byteBuffer.putInt(getIDLength());
        byteBuffer.putInt(getMSGLength());

        return byteBuffer;
    }

    private ProtocolHeader(ProtocolHeader builder) {
        this.magicNumber = MAGIC_NUMBER;
        this.protocolType = builder.protocolType;
        this.IDLength = builder.IDLength;
        this.MSGLength = builder.MSGLength;
    }

    public void setMagicNumber(byte magicNumber) {
        this.magicNumber = magicNumber;
    }

    public ProtocolHeader setProtocolType(PROTOCOL_OPT protocolType) {
        this.protocolType = protocolType;
        return this;
    }

    public ProtocolHeader setIDLength(int IDLength) {
        this.IDLength = IDLength;
        return this;
    }

    public ProtocolHeader setMSGLength(int MSGLength) {
        this.MSGLength = MSGLength;
        return this;
    }

    public byte getMagicNumber() {
        return magicNumber;
    }

    public PROTOCOL_OPT getProtocolType() {
        return protocolType;
    }

    public int getIDLength() {
        return IDLength;
    }

    public int getMSGLength() {
        return MSGLength;
    }

    public ProtocolHeader build() {
        return new ProtocolHeader(this);
    }

}
