import java.util.Date;
import java.util.HashMap;

public class Edges {

	HashMap<String,Date> targetsMap = new HashMap<String,Date>();
	Integer edgeCount = 0;
	
	public Edges(String t,Date d) {
		targetsMap.put(t, d);
		edgeCount=1;
	}

	public void addEdge(String t,Date ct){
		targetsMap.put(t, ct);
		edgeCount++;
	}
	public void removeEdge(String t){
		targetsMap.remove(t);
		edgeCount--;
	}
	public Integer getEdgeCount(){
		return edgeCount;
	}
	
}
