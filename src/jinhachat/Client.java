package jinhachat;

public class Client {

    public void clientStart() {
        //EventHandler�� ����ڰ� Ű���� �Է°� �����κ��� ���� �̺�Ʈ�� �����ϰ� ó��
        EventHandler eventHandler = new EventHandler();
        eventHandler.start();

        SystemIn systemIn = new SystemIn(eventHandler);
        systemIn.start();
    }

    public static void main(String[] args) {
        Client client = new Client();
        client.clientStart();
    }
}