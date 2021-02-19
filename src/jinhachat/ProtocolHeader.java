package jinhachat;

import java.nio.ByteBuffer;

/*
 * 통신의 총 Byte는 16byte이다.
 * |MagicN|Type|Body Length|RESERVED|
*/

public class ProtocolHeader {

    public enum PROTOCOL_OPT {
        ENTER_ROOM((byte) 0), //로그인 및 입장
        ESCAPE_ROOM((byte) 1), //퇴장
        SEND_MESSAGE((byte) 2), //채팅
        WHISPER((byte) 3), //귓속말
        SERVICE_RESPONSE((byte)4); //서버 응답?

        public static PROTOCOL_OPT valueOf(byte value) {
            for (PROTOCOL_OPT type : PROTOCOL_OPT.values()) {
                if (type.getValue() == value) return type;
            }
            return null;
        }

        private byte value;

        private PROTOCOL_OPT(byte value) {
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
    private int bodyLength;

    public ProtocolHeader() {
        setMagicNumber(MAGIC_NUMBER);
    }

    public void parse(ByteBuffer byteBuffer) {
        setMagicNumber(byteBuffer.get());
        if (getMagicNumber() != MAGIC_NUMBER) return; //나중에 예외처리
        setProtocolType(byteBuffer.get());
        setBodyLength(byteBuffer.getInt());

        while (byteBuffer.position() < HEADER_LENGTH) {
            byteBuffer.get();
        }
    }

    //전달할 변수들을 ByteBuffer에 저장
    public ByteBuffer packetize(ByteBuffer byteBuffer) {
        byteBuffer.put(getMagicNumber());
        byteBuffer.put(getProtocolType().getValue());
        byteBuffer.putInt(getBodyLength());

        //나머지 채우기
        int reservedBytes = HEADER_LENGTH - byteBuffer.position();
        byteBuffer.put(new byte[reservedBytes]);

        return byteBuffer;
    }

    public void setMagicNumber(byte magicNumber) {
        this.magicNumber = magicNumber;
    }

    public byte getMagicNumber() {
        return magicNumber;
    }

    public PROTOCOL_OPT getProtocolType() {
        return protocolType;
    }

    public void setProtocolType(byte protocolType) {
        this.protocolType = PROTOCOL_OPT.valueOf(protocolType);
    }

    public int getBodyLength() {
        return bodyLength;
    }

    public void setBodyLength(int bodyLength) {
        this.bodyLength = bodyLength;
    }
}
