package it.polimi.tiw.ecommerceCS.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import it.polimi.tiw.ecommerceCS.beans.SpendingRange;
import it.polimi.tiw.ecommerceCS.beans.Supplier;

public class SupplierDAO {
	private Connection con;

	public SupplierDAO(Connection connection) {
		this.con = connection;
	}

	public Map<Supplier, List<Object>> findSuppliersByProduct(int productID) throws SQLException {
		Map<Supplier, List<Object>> suppliers = new LinkedHashMap<Supplier, List<Object>>();
		
		String query = "SELECT * FROM (supplier JOIN soldBy ON id = soldBy.idSupp) "
				+ "LEFT JOIN spendingRange ON id = spendingRange.idSupp "
				+ "WHERE idProd = ? ORDER BY id, min ASC;";
		try (PreparedStatement pstatement = con.prepareStatement(query);) {
			pstatement.setInt(1, productID);
			try (ResultSet result = pstatement.executeQuery();) {
				int id = -1;
				List<SpendingRange> ranges = null;
				List<Object> details = null;
				Supplier supplier = null;
				while(result.next()) {
					if(id != result.getInt("id")) {
						supplier = new Supplier();
						supplier.setId(result.getInt("id"));
						supplier.setName(result.getString("name"));
						supplier.setRating(result.getInt("rating"));
						double freeShipping = result.getDouble("freeShipping");
						if (!result.wasNull()){
							  supplier.setFreeShipping(freeShipping);
						}
						
						details = new ArrayList<Object>();
						details.add(0, result.getDouble("soldBy.price"));
						
						ranges = new ArrayList<SpendingRange>();
						details.add(1, ranges);
						
						suppliers.put(supplier, details);
						
						id = supplier.getId();
					}
					if(result.getInt("min") > 0) {
						SpendingRange range = new SpendingRange();
						range.setMin(result.getInt("min"));
						range.setMax(result.getInt("max"));
						range.setPrice(result.getFloat("spendingRange.price"));
						range.setIdSupp(result.getInt("idSupp"));
						ranges.add(range);
					}
				}
			}
		}
		
		return suppliers;
	}
	
	public Supplier findSupplierByID(int suppID) throws SQLException {
		Supplier supplier = null;

		String query = "SELECT * FROM supplier  WHERE id = ?";
		try (PreparedStatement pstatement = con.prepareStatement(query);) {
			pstatement.setInt(1, suppID);
			try (ResultSet result = pstatement.executeQuery();) {
				if (result.next()) {
					supplier = new Supplier();
					supplier.setId(result.getInt("id"));
					supplier.setName(result.getString("name"));
					supplier.setRating(result.getInt("rating"));
					double freeShipping = result.getDouble("freeShipping");
					if (!result.wasNull()){
						  supplier.setFreeShipping(freeShipping);
					}
				}
			}
		}
		return supplier;
	}

}