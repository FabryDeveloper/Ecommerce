package it.polimi.tiw.ecommerceCS.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import it.polimi.tiw.ecommerceCS.beans.Product;
import it.polimi.tiw.ecommerceCS.beans.Supplier;
import it.polimi.tiw.ecommerceCS.beans.User;
import it.polimi.tiw.ecommerceCS.dao.ProductDAO;
import it.polimi.tiw.ecommerceCS.dao.SoldByDAO;
import it.polimi.tiw.ecommerceCS.dao.SupplierDAO;
import it.polimi.tiw.ecommerceCS.dao.VisualizationDAO;
import it.polimi.tiw.ecommerceCS.utils.ConnectionHandler;

@WebServlet("/GetResearch")
@MultipartConfig
public class GetResearch extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection connection = null;

	public GetResearch() {
		super();
	}

	public void init() throws ServletException {
		connection = ConnectionHandler.getConnection(getServletContext());
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		HttpSession s = request.getSession();
		User u = (User) s.getAttribute("user");

		String keyWord = request.getParameter("keyword");
		// Check parameter is present
		if (keyWord == null || keyWord.isEmpty()) {
			// Return view Home
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().println("Incorrect or missing param values");
			return;
		}

		// Check optional parameter
		String chosenProduct = request.getParameter("productid");
		Integer showProductID;

		if (chosenProduct == null) {
			showProductID = null;
		} else {
			try {
				showProductID = Integer.parseInt(chosenProduct);
			} catch (NumberFormatException | NullPointerException e) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Incorrect param values");
				return;
			}
		}

		// Extract prodcut from database
		ProductDAO productDAO = new ProductDAO(connection);

		Map<Product, Double> products = null;

		try {
			products = productDAO.findProductsByKeyWord(keyWord);

		} catch (SQLException e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
					"Not possible to recover products for this keyword");
			return;
		}

		// Product to show
		Product showProduct = null;
		double showProductPrice = 0; // Added min price also in the shown product

		// Map of suppliers and all details to show (0 -> Price, 1 -> Spending Ranges,
		// 2 -> # Products, 3 -> Products Price)
		Map<Supplier, List<Object>> showSuppliers = null;

		try {
			// Verify that showProduct is in the list of product and get it
			if (products.keySet().stream().map(p -> p.getId()).anyMatch(pId -> pId == showProductID)) {
				showProduct = products.keySet().stream().filter(p -> p.getId() == showProductID).findFirst().get();

				showProductPrice = products.get(showProduct);

				SupplierDAO suppDAO = new SupplierDAO(connection);
				SoldByDAO soldByDAO = new SoldByDAO(connection);
				VisualizationDAO visDAO = new VisualizationDAO(connection);

				Map<Integer, List<Integer>> productsInCart = ((LinkedHashMap<Integer, List<Integer>>) s
						.getAttribute("productsInCart"));

				// Suppliers and 0 -> Price 1 -> SpendingRanges
				showSuppliers = suppDAO.findSuppliersByProduct(showProductID);

				for (Supplier showS : showSuppliers.keySet()) {
					List<Object> details = showSuppliers.get(showS);

					// Verify that products in cart exists and contains the supplier
					if (productsInCart != null && productsInCart.containsKey(showS.getId())) {

						// 2 -> # Products
						details.add(2, productsInCart.get(showS.getId()).size());

						// 3 -> Products Price
						int orderPrice = 0;
						orderPrice += soldByDAO.sumOfPrices(productsInCart.get(showS.getId()), showS.getId());

						details.add(3, orderPrice);
					}
					showSuppliers.put(showS, details);
				}

				visDAO.productVisualize(showProductID, u.getEmail());
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchElementException e) {
			e.printStackTrace();
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
					"Not possible to recover this product for this id");
			return;
		}

		// Redirect to the Home page and add parameters

		// Create a GsonBuilder to enable complex map key serialization
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.enableComplexMapKeySerialization();
		Gson gson = gsonBuilder.create();
		// Create a JSON object containing multiple JSON structures
		JsonObject responseJson = new JsonObject();
		responseJson.addProperty("products", gson.toJson(products));
		responseJson.addProperty("showProduct", gson.toJson(showProduct));
		responseJson.addProperty("showSuppliers", gson.toJson(showSuppliers));
		responseJson.addProperty("showProductPrice", gson.toJson(showProductPrice));
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write(responseJson.toString());
	}

	public void destroy() {
		try {
			ConnectionHandler.closeConnection(connection);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

}
