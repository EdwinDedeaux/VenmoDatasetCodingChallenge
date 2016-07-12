import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Class to calculate the median degree from a graph of venmo transactions 
 * within a 60 second sliding window.
 * 
 * @author Edwin Dedeaux
 *
 */
public class MedianDegree{

	public MedianDegree() {
		//Currently un-modified auto-generated constructor stub
	}

	/**
	 * Main method to parse the venmo transactions input file and write the results 
	 * to the an output file.
	 * 
	 * @param args[0] contains the path for the output file
	 *        args[1] contains the path to the input file
	 * 		
	 */
	public static void main(String[] args) {

		try {
			PrintWriter pw = new PrintWriter(args[0]);
			BufferedReader br = new BufferedReader(new FileReader(
					args[1]));

			Gson gson = new Gson(); //Googles Object to de-serialize a JSON object.
			Transaction transaction;
			TreeMap<String,Edges> vertices = new TreeMap<String,Edges>();
			Date maxTime = new Date();
			boolean firstTransaction = true; 
			String str = new String();

			//
			// iterate over all the transactions in the input file.
			//
			while ((str = br.readLine()) != null) {
				//
				//populate the Edge object with the JSON entry.
				//
				try {
					transaction = gson.fromJson(str, Transaction.class);
					//
					//Check data integrity, no blank values are allowed.
					if (!transaction.validateData()) {
						continue;
					}

					if (firstTransaction){
						maxTime = transaction.getCreatedTime();
						firstTransaction = false;
					}
					if (validateAndProcessTransaction(transaction,vertices,maxTime)){
						// the transaction was verified as unique and valid
						// so determine if the maximum created_time needs resetting. 
						if (transaction.getCreatedTime().after(maxTime)) {
							maxTime = transaction.getCreatedTime();
							evaluate60SecondSlidingWindow(maxTime,vertices);
						}
						calculateMedianDegree(vertices,pw);
					}
				}
				//
				// skip over malformed json objects.
				catch (JsonSyntaxException jse){
					System.out.println("JSON de-serialization failed.");
				}
				catch (NullPointerException npe){
					System.out.println("Null pointer exception.");
				}

			}
			//
			// close the input/output files.
			br.close();
			pw.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}//end of main()

	/**
	 * This method compares the existing transaction information for data integrity, if the transaction is clean
	 * then its information is entered into the vertices map(Actor/Target relationship w/created-time).
	 *
	 * @param tr   object containing the transaction info.
	 * @param vm   Map containing the individual members and their current edge relationships
	 * @param maxTime   the upper time boundary for entries in the vertices map.
	 * 
	 */
	public static boolean validateAndProcessTransaction(Transaction tr,Map<String,Edges> vm,Date maxTime){
		//
		// determine if the created-time is outside of the time 60 second time window, 
		// if so skip this transaction.
		Date cutoffTime = getCutoffTime(maxTime);
		if (tr.getCreatedTime().before(cutoffTime))
			return true; //still need to calculate the median degree.

		//
		// determine if the actor and target of this transaction already exit in the vertices map  
		// is so update the target map for each participant, otherwise create any new entries in the vertices
		// map and initialize/update the target map for the actor/target.
		//
		if (vm.containsKey(tr.getActor())){ // actor node exits
			if (vm.containsKey(tr.getTarget())){  // target node exists
				if (vm.get(tr.getActor()).targetsMap.containsKey(tr.getTarget())){
					//update created_time possibly, not sure.
				}
				else {
					// add the target to the actors targets map and vice versa.
					vm.get(tr.getActor()).addEdge(tr.getTarget(), tr.getCreatedTime());
					vm.get(tr.getTarget()).addEdge(tr.getActor(), tr.getCreatedTime());
				}
			}
			else { // target doesn't exist.
				Edges eo = new Edges(tr.getActor(),tr.getCreatedTime());
				vm.put(tr.getTarget(), eo);
				vm.get(tr.getActor()).addEdge(tr.getTarget(), tr.getCreatedTime());
			}
		}
		//
		//
		else if (!vm.containsKey(tr.getActor())) {
			//
			// actor doesn't exists, add the target to the newly created targets map (edges object) for the actor
			// then add the actor to the vertices map.
			//
			if (vm.containsKey(tr.getTarget())){ // target exists
				// 
				// update the targets edges map to include the new actor.
				Edges eo = new Edges(tr.getTarget(),tr.getCreatedTime());
				vm.put(tr.getActor(), eo);
				vm.get(tr.getTarget()).addEdge(tr.getActor(), tr.getCreatedTime());
			}
			else { // neither actor or target exist, create and cross reference both.
				Edges eo = new Edges(tr.getTarget(),tr.getCreatedTime());
				vm.put(tr.getActor(), eo);
				Edges teo = new Edges(tr.getActor(),tr.getCreatedTime());
				vm.put(tr.getTarget(), teo);
			}
		}
		return true;
	}

	/**
	 * Method to determine if entries in the vertices map need to be removed based on 
	 * the 60 second sliding window.
	 * 
	 * @param maxTime   the upper time boundary for entries in the vertices map.
	 * @param vm   Map containing the individual members and their current edge relationships
	 * 
	 */
	public static void evaluate60SecondSlidingWindow(Date maxTime,Map<String,Edges> vm){

		class RemovalObject {
			String actor;
			String target;
		}

		Date cutoffTime = getCutoffTime(maxTime);
		ArrayList<RemovalObject> roArrayList = new ArrayList<RemovalObject>();
		//
		// iterate over each user in the vertices graph and review each of the 
		// edge relationships checking the 60 second time window, if outside of
		// the window, mark them for removal.
		//
		for (Map.Entry<String,Edges> mm : vm.entrySet()) {
			for (Map.Entry<String, Date> tme : mm.getValue().targetsMap.entrySet()) {
				if (tme.getValue().before(cutoffTime)){
					//create a removal map of actors and targets.
					RemovalObject ro = new RemovalObject(); 
					ro.actor = mm.getKey(); ro.target=tme.getKey();
					roArrayList.add(ro);
				}
			}
		}

		//  process the removal list created above.
		for (int i=0; i<roArrayList.size(); i++){
			vm.get(roArrayList.get(i).actor).removeEdge(roArrayList.get(i).target); 
			if (vm.get(roArrayList.get(i).actor).edgeCount <= 0)
				vm.remove(roArrayList.get(i).actor);
		}
	}

	/**
	 * Simple method to calculate the lower limit of the time window for entries
	 * in the vertices map.
	 * 
	 *  @param maxTime   the upper time boundary for entries in the vertices map.
	 */
	private static Date getCutoffTime(Date maxTime){
		final long ONE_MINUTE_IN_MILLIS = 60000;
		long maxTimeInMs = maxTime.getTime();
		Date cutoffTime = new Date(maxTimeInMs - ONE_MINUTE_IN_MILLIS);
		return cutoffTime;
	}

	/**
	 * Method to calculate the median for the current vertices map.
	 * 
	 * @param v  Vertices graph of relationships within the current 60 second window
	 * @param pw  PrintWriter handle for the file containing the calculated medians.
	 * 
	 */
	public static void calculateMedianDegree(Map<String,Edges> v,PrintWriter pw){
		Set<Map.Entry<String,Edges>> set = v.entrySet();
		int mdArray[] = new int[set.size()];
		int i=0;
		for (Map.Entry<String,Edges> me : set){
			Edges eo = me.getValue();
			mdArray[i++] = eo.edgeCount;
		}
		Arrays.sort(mdArray);

		if ((mdArray.length % 2) == 0){ // even # of elements in the graph
			//
			//calculate the average of the 2 center elements.
			float avg = ((mdArray[(mdArray.length/2)]) + (mdArray[(mdArray.length/2)-1]));
			float md =  avg/2;
			pw.printf("%.2f",md);
			pw.println();
		}
		else { //odd # of elements in the graph
			float md = (float) (Math.floor(mdArray.length/2));
			pw.printf("%.2f",md);
			pw.println();
		}
	}
}






