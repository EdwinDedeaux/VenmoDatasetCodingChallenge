import java.util.Date;
import java.util.HashMap;
/**
 * Class to  maintain create and maintain a list of relationships
 * between an actor and one or more targets.
 * 
 * @author Edwin Dedeaux
 *
 */
public class Edges {

	//
	// map to hold a list of targets for an each actor currently in 
	// the vertices graph.
	//
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
