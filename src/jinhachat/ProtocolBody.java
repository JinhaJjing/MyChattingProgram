package jinhachat;

import java.nio.ByteBuffer;

public class ProtocolBody {
    private String ID = "";
    private String Msg = "";

    public ByteBuffer packetize() { //(수정)매개변수 byteBuffer -> 새로운 버퍼 return
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);

        byteBuffer.put(getID().getBytes());
        byteBuffer.put(getMsg().getBytes());

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
