package jinhachat;

import java.nio.ByteBuffer;

public class ProtocolBody {
    private String ID = "";
    private String Msg = "";
    private String ChatRoom = "";
    private String targetID = "";

    public ByteBuffer packetize(ByteBuffer byteBuffer) {
        byteBuffer.put(getID().getBytes());
        byteBuffer.put(getMsg().getBytes());
        byteBuffer.put(getChatRoom().getBytes());
        byteBuffer.put(getTargetID().getBytes());

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

    public String getChatRoom() {
        return ChatRoom;
    }

    public void setChatRoom(String chatRoom) {
        ChatRoom = chatRoom;
    }

    public String getTargetID() {
        return targetID;
    }

    public void setTargetID(String targetID) {
        this.targetID = targetID;
    }
}
