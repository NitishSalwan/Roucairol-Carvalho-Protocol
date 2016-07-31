
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Client {
	private Node parentNode;
	private Map<Node, SocketMap> socketMap = null;
	private Object mRequestLock = new Object();

	public Client(Node parent) {
		this.parentNode = parent;
		initSocketConnection();
	}

	public void sendRequest() throws IOException {
		synchronized (mRequestLock) {
			parentNode.setTimeStampNow();
			parentNode.incrementClock();
			parentNode.MY_LAST_REQUEST_CLOCK = parentNode.getClock_value();

			parentNode.checkAndExecuteCriticalSection();
			
			List<Node> keysWanted = parentNode.getNodesWithoutKeys();
			if(keysWanted!=null)
			{
				for (Node node : keysWanted) {
					Message message = new Message("request", parentNode.getId(), parentNode.getClock_value());
					sendMessageToServer(node, message);
				}
			}
		}
	}

	public void sendMessageToServer(Node node, Message message) {
		PrintWriter writer = socketMap.get(node).writer;
		writer.println(message.toString());
		writer.flush();
		Logger.println("Message sent from : " + parentNode.getId() + " to " + node.getId() + " at clock "
				+ parentNode.getClock_value() + " content: " + message.toString());
	}

	public void initSocketConnection() {
		socketMap = getSocketMap();
		for (Map.Entry<Integer, Node> neighbors : parentNode.getNeighbors().entrySet()) {
			Node neighbor = neighbors.getValue();
			try {
				Socket socket = new Socket();
				socket.setKeepAlive(true);
				socket.connect(new InetSocketAddress(neighbor.getName(), neighbor.getListeningPort()), 10000);
				socketMap.put(neighbor, new SocketMap(socket, new PrintWriter(socket.getOutputStream())));
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	private Map<Node, SocketMap> getSocketMap() {
		if (socketMap == null) {
			socketMap = new HashMap<>();
		}
		return socketMap;
	}

	private static class SocketMap {
		private Socket socket;
		private PrintWriter writer;

		public SocketMap(Socket socket, PrintWriter writer) {
			super();
			this.socket = socket;
			this.writer = writer;
		}

	}
}
