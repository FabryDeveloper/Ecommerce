package it.polimi.tiw.ecommerce.beans;

import java.sql.Date;

public class Order {
	int id;
	double price;
	Date shipment;
	int idSupp;
	String emailUser;
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public double getPrice() {
		return price;
	}
	public void setPrice(double price) {
		this.price = price;
	}
	public Date getShipment() {
		return shipment;
	}
	public void setShipment(Date shipment) {
		this.shipment = shipment;
	}
	public int getIdSupp() {
		return idSupp;
	}
	public void setIdSupp(int idSupp) {
		this.idSupp = idSupp;
	}
	public String getEmailUser() {
		return emailUser;
	}
	public void setEmailUser(String emailUser) {
		this.emailUser = emailUser;
	}
	
	
	
}
