package jinhachat;

import java.nio.ByteBuffer;

public class ProtocolBody {
    private String ID = "";
    private String Msg = "";

    public ByteBuffer packetize(ByteBuffer byteBuffer) {
        byteBuffer.put(getID().getBytes());
        byteBuffer.put(getMsg().getBytes());

        byteBuffer.flip();

        return byteBuffer;
    }

    public String getID() {
        return ID;
    }

    public void setID(String ID) {
        this.ID = ID;
    }

    public String getMsg() {
        return Msg;
    }

    public void setMsg(String msg) {
        Msg = msg;
    }
}
