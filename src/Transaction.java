import java.util.Date;


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
		if ((actor.isEmpty()) || (target.isEmpty()) || (created_time == null) || (actor.equals(target)))
			return false;
		return true;
	}

}
