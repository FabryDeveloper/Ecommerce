package it.polimi.tiw.ecommerce.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import it.polimi.tiw.ecommerce.beans.Product;

public class VisualizationDAO {
	private Connection con;

	public VisualizationDAO(Connection connection) {
		this.con = connection;
	}

	public List<Product> productsVisualizedBy(String userEmail) throws SQLException {
		List<Product> products = new ArrayList<Product>();

		String query = "SELECT id, name, description, category, photo FROM visualization JOIN product ON idProd = id "
				+ "WHERE emailUser = ? ORDER BY dateTime DESC;";
		try (PreparedStatement pstatement = con.prepareStatement(query);) {
			pstatement.setString(1, userEmail);
			try (ResultSet result = pstatement.executeQuery();) {
				while (result.next()) {
					Product product = new Product();
					product.setId(result.getInt("id"));
					product.setName(result.getString("name"));
					product.setDescription(result.getString("description"));
					product.setCategory(result.getString("category"));
					product.setPhoto(Base64.getEncoder().encodeToString(result.getBytes("photo")));
					products.add(product);
				}

			}
		}
		return products;
	}
	
	public int productVisualize(int prodID, String userEmail) throws SQLException {
		int code = 0;
		String query = "INSERT into visualization (idProd, emailUser, dateTime) VALUES(?, ?, DEFAULT) ON DUPLICATE KEY UPDATE dateTime = DEFAULT;";
		try (PreparedStatement pstatement = con.prepareStatement(query);) {
			pstatement.setInt(1, prodID);
			pstatement.setString(2, userEmail);
			code = pstatement.executeUpdate();
		}
		return code;
	}
}
