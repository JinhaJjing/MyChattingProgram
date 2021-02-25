package jinhachat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;

public class Client {
    private String ID;
    private boolean isLoggedIn = false;

    public void clientStart() {
        try (SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress("localhost", 15000))) {
            //EventHandler�� ����ڰ� Ű���� �Է°� �����κ��� ���� �̺�Ʈ�� �����ϰ� ó��
            Thread eventHandler = new Thread(new EventHandler(socketChannel, this));
            eventHandler.start();

            Thread systemIn = new Thread(new SystemIn(socketChannel, this));
            systemIn.start();

        } catch (IOException e) {
            System.out.println("������ ������ ����Ǿ����ϴ�.");
            e.printStackTrace();
        }
    }

    public String getID() {
        return ID;
    }

    public void setID(String ID) {
        this.ID = ID;
    }

    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    public void setLoggedIn(boolean loggedIn) {
        isLoggedIn = loggedIn;
    }

    public static void main(String[] args) {
        Client client = new Client();
        client.clientStart();
    }
}