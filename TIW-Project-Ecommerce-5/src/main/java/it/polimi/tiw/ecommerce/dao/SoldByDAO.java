package it.polimi.tiw.ecommerce.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class SoldByDAO {
	private Connection con;

	public SoldByDAO(Connection connection) {
		this.con = connection;
	}

	public double sumOfPrices(List<Integer> productsIDs, int suppID) throws SQLException {
		
		 Map<Integer, Integer> idCountMap = new HashMap<>();
         for (int id : productsIDs) {
             idCountMap.put(id, idCountMap.getOrDefault(id, 0) + 1);
         }
         
		
		String query = "SELECT idProd, price FROM soldBy WHERE idSupp = ? AND idProd IN (";
		
		// Aggiunge un segnaposto "?" per ogni ID nella lista
        for (int i = 0; i < idCountMap.keySet().size(); i++) {
            query += "?";
            if (i < idCountMap.keySet().size() - 1) {
                query += ",";
            }
        }
        
        query += ");";
        
        
        double sum_price = 0;
        
		try (PreparedStatement pstatement = con.prepareStatement(query);) {
			pstatement.setInt(1, suppID);
			
			// Imposta i parametri per ogni ID nella lista
			int i = 2;
			for (int id : idCountMap.keySet()) {
                pstatement.setInt(i, id);
                i++;
            }
            
			try (ResultSet result = pstatement.executeQuery();) {
				while (result.next()) {
					sum_price += result.getDouble("price") * idCountMap.get(result.getInt("idProd"));
				}
			}

		}
		
		
		return sum_price;
	}
}