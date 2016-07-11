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

public class MedianDegree{


	public MedianDegree() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param args
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
			br.close();
			pw.close();
		}
		catch (IOException e) {
			System.out.println("Bad Transaction");
			e.printStackTrace();
		}
	}//end of main()


	public static boolean validateAndProcessTransaction(Transaction tr,Map<String,Edges> vm,Date maxTime){
		//
		// outside of the time window, discard.
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


	public static void evaluate60SecondSlidingWindow(Date maxTime,Map<String,Edges> vm){

		class RemovalObject {
			String actor;
			String target;
		}

		Date cutoffTime = getCutoffTime(maxTime);
		ArrayList<RemovalObject> roArrayList = new ArrayList<RemovalObject>();

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

		//
		// 
		//
		for (int i=0; i<roArrayList.size(); i++){
			vm.get(roArrayList.get(i).actor).removeEdge(roArrayList.get(i).target); 
			if (vm.get(roArrayList.get(i).actor).edgeCount <= 0)
				vm.remove(roArrayList.get(i).actor);
		}
	}


	private static Date getCutoffTime(Date maxTime){
		final long ONE_MINUTE_IN_MILLIS = 60000;
		long maxTimeInMs = maxTime.getTime();
		Date cutoffTime = new Date(maxTimeInMs - ONE_MINUTE_IN_MILLIS);
		return cutoffTime;
	}

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






