package it.polimi.tiw.ecommerce.beans;

public class Supplier {
	int id;
	String name;
	int rating;
	Double freeShipping;
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public int getRating() {
		return rating;
	}
	public void setRating(int rating) {
		this.rating = rating;
	}
	public Double getFreeShipping() {
		return freeShipping;
	}
	public void setFreeShipping(Double freeShipping) {
		this.freeShipping = freeShipping;
	}
	
}
