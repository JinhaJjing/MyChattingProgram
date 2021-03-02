package jinhachat;

public class Client {

    public void clientStart() {
        try (SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress("localhost", 15000))) {
            //EventHandler는 사용자가 키보드 입력과 서버로부터 오는 이벤트를 감지하고 처리
            Thread eventHandler = new Thread(new EventHandler(socketChannel, this));
            eventHandler.start();

            Thread systemIn = new Thread(new SystemIn(socketChannel, this));
            systemIn.start();

        } catch (IOException e) {
            System.out.println("서버와 연결이 종료되었습니다.");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Client client = new Client();
        client.clientStart();
    }
}