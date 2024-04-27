package it.polimi.tiw.ecommerce.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;

public class CompositionDAO {
	private Connection con;

	public CompositionDAO(Connection connection) {
		this.con = connection;
	}

	public int createComposition(int orderID, List<Integer> productsIDs) throws SQLException {
		int code = 0;
		
		// Rigestisce la lista di productsIDs togliendo le ripetizioni e indicando la conta del prodotto
        Map<Integer, Integer> countMap = new LinkedHashMap<>();

        for (int prodID : productsIDs) {
            countMap.put(prodID, countMap.getOrDefault(prodID, 0) + 1);
        }
        
        for (Map.Entry<Integer, Integer> entry : countMap.entrySet()) {
			int prodID = entry.getKey();
			int count = entry.getValue();
			
			String query = "INSERT into composition (idOrd, idProd, numProd) VALUES(?, ?, ?)";
			try (PreparedStatement pstatement = con.prepareStatement(query);) {
				pstatement.setInt(1, orderID);
				pstatement.setInt(2, prodID);
				pstatement.setInt(3, count);
				code += pstatement.executeUpdate();
			}
		}
		
		
		return code;
	}
}