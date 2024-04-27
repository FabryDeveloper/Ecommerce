package it.polimi.tiw.ecommerceCS.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import it.polimi.tiw.ecommerceCS.beans.Product;

import java.util.Base64;
import java.util.HashMap;

public class ProductDAO {
	private Connection con;

	public ProductDAO(Connection connection) {
		this.con = connection;
	}

	public List<Product> fiveProductsForUser(String userEmail) throws SQLException {
		List<Product> products = null;

		VisualizationDAO visDAO = new VisualizationDAO(con);
		int visual = 0;
		try {
			products = visDAO.productsVisualizedBy(userEmail);
			visual = products.size(); 
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		int offer = 0;
		if (products.size() < 5) {
			String query = "SELECT * FROM product  WHERE category = 'Offer'";
			try (PreparedStatement pstatement = con.prepareStatement(query);) {
				try (ResultSet result = pstatement.executeQuery();) {
					while (result.next()) {
						Product product = new Product();
						product.setId(result.getInt("id"));
						product.setName(result.getString("name"));
						product.setDescription(result.getString("description"));
						product.setCategory(result.getString("category"));
						product.setPhoto(Base64.getEncoder().encodeToString(result.getBytes("photo")));
						products.add(product);
						offer++;
					}
				}
			}
		}
		
		// Rimuove duplicati
		for(int i = 0; i<visual; i++) {
			for(int j=0; j<offer; j++) {
				if(products.get(i).getId() == products.get(visual+j).getId()) {
					products.remove(visual+j);
					offer--;
				}
			}
		}
		
		if(products.size()<=5) {
			return products;
		}else {
			return products.subList(0, 5);
		}
	}

	public Map<Product, Double> findProductsByKeyWord(String keyWord) throws SQLException {
		Map<Product, Double> products = new LinkedHashMap<Product, Double>();

		String query = "SELECT id, name, description, category, photo, MIN(price) as min_price "
				+ "FROM product JOIN soldBy ON id = idProd WHERE name LIKE ? OR description LIKE ? "
				+ "GROUP BY id, name, description, category, photo "
				+ "ORDER BY min_price ASC;";
		try (PreparedStatement pstatement = con.prepareStatement(query);) {
			pstatement.setString(1, "%" + keyWord + "%");
			pstatement.setString(2, "%" + keyWord + "%");
			try (ResultSet result = pstatement.executeQuery();) {
				while (result.next()) {
					Product product = new Product();
					product.setId(result.getInt("id"));
					product.setName(result.getString("name"));
					product.setDescription(result.getString("description"));
					product.setCategory(result.getString("category"));
					product.setPhoto(Base64.getEncoder().encodeToString(result.getBytes("photo")));
					products.put(product, result.getDouble("min_price"));
				}
			}
		}

		return products;
	}

	public Map<Product, Integer> findProductsByIDs(List<Integer> productsIDs) throws SQLException {
		 Map<Integer, Integer> idCountMap = new HashMap<>();
         for (int id : productsIDs) {
             idCountMap.put(id, idCountMap.getOrDefault(id, 0) + 1);
         }
         
         String query = "SELECT * FROM product  WHERE id IN (";
		
		// Aggiunge un segnaposto "?" per ogni ID nella lista
        for (int i = 0; i < idCountMap.keySet().size(); i++) {
            query += "?";
            if (i < idCountMap.keySet().size() - 1) {
                query += ",";
            }
        }
        
        query += ");";
        
        Map<Product, Integer> products = new LinkedHashMap<Product, Integer>();
        
		try (PreparedStatement pstatement = con.prepareStatement(query);) {
			// Imposta i parametri per ogni ID nella lista
			int i = 1;
			for (int id : idCountMap.keySet()) {
                pstatement.setInt(i, id);
                i++;
            }
			try (ResultSet result = pstatement.executeQuery();) {
				while (result.next()) {
					Product product = new Product();
					product.setId(result.getInt("id"));
					product.setName(result.getString("name"));
					product.setDescription(result.getString("description"));
					product.setCategory(result.getString("category"));
					product.setPhoto(Base64.getEncoder().encodeToString(result.getBytes("photo")));
					
					products.put(product, idCountMap.get(product.getId()));
				}
			}
		}
		return products;
	}

}
