package it.polimi.tiw.ecommerceCS.exceptions;

public class BadOrder extends Exception {
	private static final long serialVersionUID = 1L;

	public BadOrder(String message) {
		super(message);
	}
}