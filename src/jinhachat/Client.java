package jinhachat;

public class Client {

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

    public static void main(String[] args) {
        Client client = new Client();
        client.clientStart();
    }
}