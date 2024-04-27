package it.polimi.tiw.ecommerce.beans;

public class User {
	private String email;
	private String name;
	private String surname;
	private String address;

	public String getEmail() {
		return email;
	}

	public String getName() {
		return name;
	}

	public String getSurname() {
		return surname;
	}
	
	public String getAddress() {
		return address;
	}
	
	public void setEmail(String em) {
		email = em;
	}

	public void setName(String n) {
		name = n;
	}

	public void setSurname(String sn) {
		surname = sn;
	}
	
	public void setAddress(String a) {
		address = a;
	}

}
