package jinhachat;

public class Client {

    public void clientStart() {
        ClientInfo clientInfo = new ClientInfo();

        //EventHandler�� ����ڰ� Ű���� �Է°� �����κ��� ���� �̺�Ʈ�� �����ϰ� ó��
        EventHandler eventHandler = new EventHandler(clientInfo);
        eventHandler.start();

        SystemIn systemIn = new SystemIn(eventHandler);
        systemIn.start();
    }

    public static void main(String[] args) {
        Client client = new Client();
        client.clientStart();
    }
}