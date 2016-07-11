import java.util.Date;

/**
 * Class used for deserializing a JSON object containing various
 * pieces of information for a venmo transaction.
 * 
 */
public class Transaction {

	public Transaction() {
		// this object gets initialized via the Gson object method .fromJson(string, Transaction.class)
	}
	private String actor;
	private String target;
	private Date created_time;

	public String getActor() {
		return actor;
	}
	public String getTarget() {
		return target;
	}
	public Date getCreatedTime() {
		return created_time;
	}
	public boolean validateData(){
		if ((actor.isEmpty()) || (target.isEmpty()) || (created_time == null))
			return false;
		return true;
	}

}
