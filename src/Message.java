public class Message {
	private String content;
	private int senderNodeId;
	private int clock_value;

	public Message(String content, int senderNodeId, int clock_value) {
		this.content = content;
		this.senderNodeId = senderNodeId;
		this.clock_value = clock_value;
	}

	public Message(String messageStr) {
		String[] message_array = messageStr.split("\\s+");
		this.content = message_array[0].trim();
		this.senderNodeId = Integer.parseInt(message_array[1].trim());
		this.clock_value = Integer.parseInt(message_array[2].trim());
	}

	public void setClock_value(int clock_value) {
		this.clock_value = clock_value;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public int getSenderNodeId() {
		return senderNodeId;
	}

	public void setSenderNodeId(int senderNodeId) {
		this.senderNodeId = senderNodeId;
	}

	public int getTime() {
		return clock_value;
	}

	public void setTime(int clock_value) {
		this.clock_value = clock_value;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return content + " " + senderNodeId + " " + clock_value;
	}
}
