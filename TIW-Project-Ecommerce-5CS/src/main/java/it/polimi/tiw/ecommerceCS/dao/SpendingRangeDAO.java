package it.polimi.tiw.ecommerceCS.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SpendingRangeDAO {
	private Connection con;

	public SpendingRangeDAO(Connection connection) {
		this.con = connection;
	}

	public Double shipmentPrice(int suppID, int numProd) throws SQLException {
		double price = 0;
		
		String query = "SELECT price FROM spendingRange WHERE idSupp = ? AND min <= ? AND (max >= ? OR max IS NULL);";
		try (PreparedStatement pstatement = con.prepareStatement(query);) {
			pstatement.setInt(1, suppID);
			pstatement.setInt(2, numProd);
			pstatement.setInt(3, numProd);
			try (ResultSet result = pstatement.executeQuery();) {
				if (result.next()) {
					price = result.getDouble("price");
				}

			}
		}
		
		return price;
	}
}