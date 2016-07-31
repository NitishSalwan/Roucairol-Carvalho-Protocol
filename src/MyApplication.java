
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class MyApplication {

	private final int TOTAL_NODES;
	HashMap<Integer, Node> host_to_port = new HashMap<Integer, Node>();
	private Node node;
	
	private int interRequestDelay;
	public static int criticalSectionTime;
	private int noOfRequests;

	public MyApplication(final int node_id) throws IOException {

		FileInputStream fis_lan = null;
		File file = new File("config1.txt");
		fis_lan = new FileInputStream(file);
		// while((f2=fis_lan.getChannel().tryLock())==null){}
		BufferedReader br = new BufferedReader(new InputStreamReader(fis_lan));

		String first_line = br.readLine();
		String[] messages_props = first_line.split("\\s+");
		TOTAL_NODES = Integer.parseInt(messages_props[0]);

		System.out.println(interRequestDelay=Integer.parseInt(messages_props[1]));
		System.out.println(criticalSectionTime=Integer.parseInt(messages_props[2]));
		System.out.println(noOfRequests=Integer.parseInt(messages_props[3]));

		String currentline;
		int count = Integer.parseInt(messages_props[0]);
		int i = 0;

		while (((currentline = br.readLine()) != null) && count > i) {
			if (!currentline.equals("")) {
				String[] words = currentline.split("\\s+");

				if (Integer.parseInt(words[0]) == node_id) {
					node = new Node(node_id);
					Logger.setParentNode(node);
					node.setListeningPort(Integer.parseInt(words[2]));
					node.setName(words[1]);
					System.out.println("Node :" + node.toString());

				} else {
					host_to_port.put(Integer.parseInt(words[0]),
							new Node(Integer.parseInt(words[0]), words[1], Integer.parseInt(words[2])));
							// host_to_node.put(words[1],
							// Integer.parseInt(words[0]));

					// aos2.cs_request_inner.put(Integer.parseInt(words[0]),
					// false);
				}
				i++;
			}

		}
		node.setNeighbors(host_to_port);
		node.initNodeKeys();
		HashMap<Node,Boolean> tempMap =new HashMap<Node,Boolean>();
		for(Map.Entry<Integer, Node> neighbour : node.getNeighbors().entrySet())
		{
			tempMap.put(neighbour.getValue(),false);
		}
		
		br.close();
	}

	public static void main(String[] args) throws Exception {
		MyApplication myApp = new MyApplication(Integer.parseInt(args[0]));
		myApp.node.startConnections();
		
		try{
			Thread.sleep(5000);
		}catch(Exception ex){
			ex.printStackTrace();
		}
		
		Client client = myApp.node.getClient();
		Server server = myApp.node.getServer();
		
		//		client.initSocketConnection();
		
		
		for(int i=0;i<myApp.noOfRequests;i++)
		{
		client.sendRequest();
		
		Thread.sleep(myApp.interRequestDelay);
		
		}
		
		
		
		
		
		
	}
}
