
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Map;

public class Server implements Runnable {
	private Node parentNode;
	private ServerSocket serverSocket;
	private Socket socket;

	public Server(Node parent) {
		parentNode = parent;
	}

	@Override
	public void run() {
		try {
			serverSocket = new ServerSocket(parentNode.getListeningPort());
			Logger.println("Server up for Node :" + parentNode.toString());
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		while (true) {
			try {
				// sctpChannel = sctpServerChannel.accept();
				socket = serverSocket.accept();
				Logger.println("Request accepted at Node : " + parentNode.toString());
				new Thread(new RequestHandler(socket, parentNode)).start();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}
}

class RequestHandler implements Runnable {
	private Node node;
	private Socket socket;
	private BufferedReader reader;

	public RequestHandler(Socket socket, Node node) throws Exception {
		this.node = node;
		this.socket = socket;
		reader = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
	}

	public void run() {
		String messageStr = null;
		try {
			while ((messageStr = reader.readLine()) != null) {
				node.incrementClock();
				Message message = new Message(messageStr);
				Logger.println("Message received at node " + node.getId() + " : " + message.toString()
						+ " & Deferred Queue Size :" + node.getQueueSize());
				processMessage(message);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}

	private void processMessage(Message message) throws IOException {
		if (message.getContent().trim().equals("request")) {
			Logger.println("Message received as request at " + node.getId() + " from : " + message.getSenderNodeId());
			if (node.inCriticalSection) {
				System.out.println(node.getId() + " is in critical section. Request will be deffered");
				node.addDeferRequest(message);
			}
			else if (node.MY_LAST_REQUEST_CLOCK == Integer.MAX_VALUE)
			{
				Message temp_message = new Message("returnkey", node.getId(), node.getClock_value());
				node.sendMessageToServer(node.getNeighbors().get(message.getSenderNodeId()), temp_message);
				node.removeKey(node.getNeighbors().get(message.getSenderNodeId()));
				Logger.println("Returned Key to : " + message.getSenderNodeId());
				
			}else if(node.MY_LAST_REQUEST_CLOCK < message.getTime())
			{
				System.out.println(node.getId() + " has a request with high priority. Request will be deffered");
				node.addDeferRequest(message);
			}
			else if (node.MY_LAST_REQUEST_CLOCK > message.getTime()) {
				Logger.println("Case 1 at : " + node.getId() + " from  : " + message.getSenderNodeId());
				Message temp_message = new Message("returnkey", node.getId(), node.getClock_value());
				node.sendMessageToServer(node.getNeighbors().get(message.getSenderNodeId()), temp_message);
				node.removeKey(node.getNeighbors().get(message.getSenderNodeId()));
				Logger.println("Returned Key to : " + message.getSenderNodeId());
				temp_message = new Message("request", node.getId(), node.getClock_value());
				node.sendMessageToServer(node.getNeighbors().get(message.getSenderNodeId()), temp_message);
				Logger.println("Asked for key back from : " + message.getSenderNodeId());
			} else if (node.MY_LAST_REQUEST_CLOCK == message.getTime()) {
				Logger.println("Case 2 at : " + node.getId() + " from : " + message.getSenderNodeId());
				if (node.getId() < message.getSenderNodeId()) {
					node.addDeferRequest(message);
					Logger.println("Request Defered from : " + message.getSenderNodeId());
				} else if (node.getId() > message.getSenderNodeId()) {

					Message temp_message = new Message("returnkey", node.getId(), node.getClock_value());
					node.sendMessageToServer(node.getNeighbors().get(message.getSenderNodeId()), temp_message);
					node.removeKey(node.getNeighbors().get(message.getSenderNodeId()));
					Logger.println("Returned Key to : " + message.getSenderNodeId());
					temp_message = new Message("request", node.getId(), node.getClock_value());

					node.sendMessageToServer(node.getNeighbors().get(message.getSenderNodeId()), temp_message);
					Logger.println("Asked for key back from : " + message.getSenderNodeId());
				} else {
					throw new UnsupportedOperationException("Node Ids cant't be same");
				}
			}
		} else if (message.getContent().trim().equals("returnkey")) {
			Logger.println(
					"Message received as returnkey at : " + node.getId() + " from : " + message.getSenderNodeId());
			node.addKey(node.getNeighbors().get(message.getSenderNodeId()));
			node.checkAndExecuteCriticalSection();
	
		} else {
			throw new UnsupportedOperationException("No method with corresponding message");
		}
	}
}
