package jinhachat;

import java.nio.ByteBuffer;

/*
 * 통신의 총 Byte는 16byte이다.
 * |MagicNumber|Message Type|Body Length|RESERVED|
 */

public class ProtocolHeader {

    public enum PROTOCOL_OPT {
        REQ_LOGIN((byte) 0), //로그인 요청
        RES_LOGIN_SUCCESS((byte) 1), //로그인 성공
        RES_LOGIN_FAIL((byte) 2), //로그인 실패
        NOTICE_LOGIN((byte)3), //로그인 알림
        NOTICE_EXIT((byte)4), //퇴장 알림
        REQ_CHAT((byte) 5), //채팅 요청
        NOTICE_CHAT((byte) 6), //전체 알림
        REQ_WHISPER((byte)7), //귓속말 요청
        RES_WHISPER_FAIL((byte)8), //귓속말 실패
        NOTICE_WHISPER((byte)9); //귓속말 알림

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
    private int bodyLength = 0;

    public ProtocolHeader() {
        this.magicNumber = MAGIC_NUMBER;
    }

    public void parse(ByteBuffer byteBuffer) {
        setMagicNumber(byteBuffer.get());
        if (getMagicNumber() != MAGIC_NUMBER) return; //나중에 예외처리
        setProtocolType(PROTOCOL_OPT.valueOf(byteBuffer.get()));
        setBodyLength(byteBuffer.getInt());

        if (byteBuffer.position() < HEADER_LENGTH) { //(수정)bytebuffer.get여러개 -> position을 옮기는 방법
            byteBuffer.position(HEADER_LENGTH);
        }
    }

    //전달할 변수들을 ByteBuffer에 저장
    public ByteBuffer packetize() { //(수정)매개변수 byteBuffer -> 새로운 버퍼 return
        ByteBuffer byteBuffer = ByteBuffer.allocate(HEADER_LENGTH);

        byteBuffer.put(getMagicNumber());
        byteBuffer.put((byte) getProtocolType().ordinal());
        byteBuffer.putInt(getBodyLength());

        for(int i=0;i<10;i++)
            byteBuffer.put((byte)0);

        byteBuffer.flip();

        return byteBuffer;
    }

    private ProtocolHeader(ProtocolHeader builder) {
        this.magicNumber = builder.MAGIC_NUMBER;
        this.protocolType = builder.protocolType;
    }

    public void setMagicNumber(byte magicNumber) {
        this.magicNumber = magicNumber;
    }

    public void setProtocolType(PROTOCOL_OPT protocolType) {
        this.protocolType = protocolType;
    }

    public void setBodyLength(int bodyLength) {
        this.bodyLength = bodyLength;
    }

    public byte getMagicNumber() {
        return magicNumber;
    }

    public PROTOCOL_OPT getProtocolType() {
        return protocolType;
    }

    public int getBodyLength() {
        return bodyLength;
    }

}
