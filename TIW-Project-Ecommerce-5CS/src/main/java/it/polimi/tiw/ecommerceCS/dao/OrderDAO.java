package it.polimi.tiw.ecommerceCS.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;

import java.util.Random;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import it.polimi.tiw.ecommerceCS.beans.Order;
import it.polimi.tiw.ecommerceCS.beans.Product;
import it.polimi.tiw.ecommerceCS.beans.Supplier;
import it.polimi.tiw.ecommerceCS.exceptions.BadOrder;

public class OrderDAO {
	private Connection con;

	public OrderDAO(Connection connection) {
		this.con = connection;
	}

	public int createOrder(String userEmail, int suppID, List<Integer> productsIDs) throws SQLException, BadOrder{
		int code = 0;

		SoldByDAO soldByDAO = new SoldByDAO(con);
		double price = soldByDAO.sumOfPrices(productsIDs, suppID);
		
		SupplierDAO suppDAO = new SupplierDAO(con);
		Supplier supp = suppDAO.findSupplierByID(suppID);
		if (supp.getFreeShipping() == null || supp.getFreeShipping() > price) {
			SpendingRangeDAO rangeDAO = new SpendingRangeDAO(con);
			int numProd = productsIDs.size();
			price += rangeDAO.shipmentPrice(suppID, numProd);
		}

		java.util.Date oggi = new java.util.Date();

		// Converte la data odierna in java.sql.Date
		Date dataOdierna = new Date(oggi.getTime());
		
		Random rand = new Random();
		// Crea una nuova data successiva all'odierna casuale (entro 7 giorni)
		long giornoSuccessivo = dataOdierna.getTime() + rand.nextLong(24 * 60 * 60 * 1000, 7 * 24 * 60 * 60 * 1000); // Aggiunge num di giorni casuali in millisecondi
		Date shipment = new Date(giornoSuccessivo);
		
		CompositionDAO compositionDAO = new CompositionDAO(con);
		
		// Doppia Insert con Order e Composition
		con.setAutoCommit(false);
		
		String query = "INSERT into ecommerce.order (price, shipment, idSupp, emailUser) VALUES(?, ?, ?, ?)";
		try (PreparedStatement pstatement = con.prepareStatement(query, PreparedStatement.RETURN_GENERATED_KEYS/*Necessario per ottenere l'OrderID*/);) {
			pstatement.setDouble(1, price);
			pstatement.setDate(2, shipment);
			pstatement.setInt(3, suppID);
			pstatement.setString(4, userEmail);
			code = pstatement.executeUpdate();
			
			ResultSet rs = pstatement.getGeneratedKeys();
			int orderID = -1;
		    if (rs.next()) {
		        orderID = rs.getInt(1);
		    } else {
		    	throw new BadOrder("Order ID not received from db");
		    }
		    
		    if(orderID != -1) {
		    	code += compositionDAO.createComposition(orderID, productsIDs);
		    	con.commit();
		    } else {
		    	throw new BadOrder("Wrong Order ID received from db");
		    }
			
		} catch (SQLException | BadOrder e) {
			con.rollback();
			throw e;
		} finally {
			con.setAutoCommit(true);
		}

		return code;
	}

	public Map<Order, List<Object>> findOrdersForUser(String userEmail) throws SQLException {
		Map<Order, List<Object>> orders = new LinkedHashMap<Order, List<Object>>();
		
		
		String query = "SELECT * FROM ecommerce.order, composition, supplier, product, user "
				+ "WHERE ecommerce.order.id = composition.idOrd AND ecommerce.order.idSupp = supplier.id AND "
				+ "composition.idProd = product.id AND ecommerce.order.emailUser = user.email AND emailUser = ? "
				+ "ORDER BY ecommerce.order.shipment DESC, ecommerce.order.id ASC;"; // Ordina per data decrescente
		try (PreparedStatement pstatement = con.prepareStatement(query);) {
			pstatement.setString(1, userEmail);
			try (ResultSet result = pstatement.executeQuery();) {
				int id = -1;
				Map<Product, Integer> products = null;
				List<Object> details = null;
				Order ord = null;
				while(result.next()) {
					if(id != result.getInt("order.id")) {
						ord = new Order();
						ord.setId(result.getInt("order.id"));
						ord.setPrice(result.getDouble("order.price"));
						ord.setShipment(result.getDate("order.shipment"));
						ord.setIdSupp(result.getInt("order.idSupp"));
						ord.setEmailUser(result.getString("order.emailUser"));
						
						details = new ArrayList<Object>();
						
						products = new LinkedHashMap<Product, Integer>();
						details.add(0, products);
						
						details.add(1, result.getString("supplier.name"));
						details.add(2, result.getString("user.address"));
						
						orders.put(ord, details);
						
						id = ord.getId();
					}
					
					Product product = new Product();
					product.setId(result.getInt("product.id"));
					product.setName(result.getString("product.name"));
					product.setDescription(result.getString("product.description"));
					product.setCategory(result.getString("product.category"));
					product.setPhoto(Base64.getEncoder().encodeToString(result.getBytes("product.photo")));
					products.put(product, result.getInt("composition.numProd"));
				}

			}
		}

		return orders;
	}
}