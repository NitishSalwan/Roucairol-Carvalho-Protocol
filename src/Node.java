
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Node {
	// private static final Object mLock = new Object();
	/**
	 * Only for Criticaal Section
	 */
	private static final Object mCriticalSectionLock = new Object();
	private int _id;
	private String name;
	private int listeningPort;
	private int clock_value = -1;
	private String csRequesTime;
	public volatile boolean inCriticalSection = false;

	public volatile int MY_LAST_REQUEST_CLOCK = Integer.MAX_VALUE;
	private HashMap<Node, Boolean> nodeKeys = null;
	private List<Message> deferedRequests = null;

	private Server server;
	private Client client;

	private Map<Integer, Node> neighbors = null;

	// private static final Object mObject = new Object();

	// Message Comparator
	Comparator<Message> comparator = new Comparator<Message>() {
		@Override
		public int compare(Message m1, Message m2) {
			if ((m1.getTime() != m2.getTime())) {
				if (m1.getTime() > m2.getTime()) {
					return 1;
				} else {
					return -1;
				}
			} else {

				if (m1.getSenderNodeId() > m2.getSenderNodeId()) {
					return 1;
				} else {
					return -1;
				}
			}
		}
	};

	// Current Instance
	public Node(int nodeId) {
		_id = nodeId;
		clock_value = 0;
		neighbors = new HashMap<>();
		deferedRequests = new LinkedList<Message>();
		nodeKeys = new HashMap<>();
	}

	public Node(int nodeId, String name, int listenPort) {
		_id = nodeId;
		this.name = name;
		listeningPort = listenPort;
	}

	/**
	 * Generates Node Keys for 'jth' Node jth node has keys from j+1 till N
	 */
	public void initNodeKeys() {
		/*
		if(_id == 0)
		{
			nodeKeys.put(neighbors.get(1), true);
			nodeKeys.put(neighbors.get(2), false);
		}else if(_id == 1)
		{
			nodeKeys.put(neighbors.get(2), true);
			nodeKeys.put(neighbors.get(0), false);
		}else
		{
			nodeKeys.put(neighbors.get(1), false);
			nodeKeys.put(neighbors.get(0), true);
		}
		*/
		for (int i = 0; i < neighbors.size(); i++) {
			if (i == _id) {
				continue;
			} else if (i < _id)
				nodeKeys.put(neighbors.get(i), false);
			else if (i > _id)
				nodeKeys.put(neighbors.get(i), true);
		}
		
		System.out.println("Node Keys");
		for (Map.Entry<Node, Boolean> map : nodeKeys.entrySet()) {
			System.out.println(map.getKey() + " " + map.getValue());

		}
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setListeningPort(int listeningPort) {
		this.listeningPort = listeningPort;
	}

	public int getId() {
		return _id;
	}

	public String getName() {
		return name;
	}

	public int getListeningPort() {
		return listeningPort;
	}

	public int getClock_value() {
		return clock_value;
	}

	public void incrementClock() {
		++clock_value;
	}

	public int getQueueSize() {
		return deferedRequests.size();
	}

	@Override
	public String toString() {
		return _id + " " + name + " " + listeningPort;
	}

	public Map<Integer, Node> getNeighbors() {
		return neighbors;
	}

	public void setNeighbors(Map<Integer, Node> host_to_port) {
		neighbors = host_to_port;
	}

	public void putMessageInQueue(Message message) {
		deferedRequests.add(message);
	}

	public synchronized void startConnections() {
		server = new Server(this);
		new Thread(server).start();

		try {
			Thread.sleep(2000);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		// Start Client to send Requests
		new Thread(new Runnable() {
			@Override
			public void run() {
				client = new Client(Node.this);
			}
		}).start();

	}

	public Server getServer() {
		return server;
	}

	public Client getClient() {
		return client;
	}

	private void executeCriticalSection() {
		Logger.println("Critical Section for Node : " + this._id + " executed");
	}

	public void checkAndExecuteCriticalSection() {
		synchronized (mCriticalSectionLock) {
			if (getNodesWithoutKeys() == null) {
				Logger.csLog("Critical Section execution started at :: " + System.currentTimeMillis());
				inCriticalSection = true;
				executeCriticalSection();
				inCriticalSection = false;
				Logger.csLog("Critical Section execution finished at :: " + System.currentTimeMillis());
				afterCriticalExecution();
			} else {
				Logger.println("Didn't have all keys, couldn't execute Critical Secction");
			}
		}
	}

	private void afterCriticalExecution() {
		List<Node> nodesRequiringKeys = checkForKeysRequests();
		for (Node node : nodesRequiringKeys) {
			// Send the key to the node
			Message message = new Message("returnkey", getId(), getClock_value());
			client.sendMessageToServer(node, message);
			removeKey(node);
		}
		clearDeferList();
		MY_LAST_REQUEST_CLOCK = Integer.MAX_VALUE;
	}

	public void removeMessageFromQueue(Message messageToRemove) {
		for (Message tempMessage : deferedRequests) {
			if (tempMessage.getSenderNodeId() == messageToRemove.getSenderNodeId()) {
				deferedRequests.remove(tempMessage);
			}
		}
	}

	public void setTimeStampNow() {
		csRequesTime = Long.toString(System.currentTimeMillis());
	}

	public String getTimeStampNow() {
		return csRequesTime;
	}

	public void emptyForNewRequest() {
		deferedRequests.clear();
	}

	public List<Node> getNodesWithoutKeys() {
		List<Node> nodesWithoutKeys = new ArrayList<>();
		for (Map.Entry<Node, Boolean> map : nodeKeys.entrySet()) {
			if (!map.getValue()) {
				nodesWithoutKeys.add(map.getKey());
			}
		}
		return nodesWithoutKeys.isEmpty() ? null : nodesWithoutKeys;
	}

	public List<Node> checkForKeysRequests() {
		List<Node> newNode = new ArrayList<>();
		for (Message message : deferedRequests) { // Integer id
			newNode.add(neighbors.get(message.getSenderNodeId()));
		}
		return newNode;
	}

	public void removeKey(Node node) {
		nodeKeys.put(node, false);
	}

	public void clearDeferList() {
		// TODO Auto-generated method stub
		deferedRequests.clear();

	}

	public void addDeferRequest(Message message) {
		// TODO Auto-generated method stub
		deferedRequests.add(message);
	}

	public void sendMessageToServer(Node node, Message message) {
		// TODO Auto-generated method stub
		client.sendMessageToServer(node, message);
	}

	public void addKey(Node node) {
		// TODO Auto-generated method stub
		nodeKeys.put(node, true);
	}

}
