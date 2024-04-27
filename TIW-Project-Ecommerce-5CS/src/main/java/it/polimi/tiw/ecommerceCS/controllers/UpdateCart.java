package it.polimi.tiw.ecommerceCS.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import it.polimi.tiw.ecommerceCS.beans.Product;
import it.polimi.tiw.ecommerceCS.beans.Supplier;
import it.polimi.tiw.ecommerceCS.dao.ProductDAO;
import it.polimi.tiw.ecommerceCS.dao.SoldByDAO;
import it.polimi.tiw.ecommerceCS.dao.SpendingRangeDAO;
import it.polimi.tiw.ecommerceCS.dao.SupplierDAO;
import it.polimi.tiw.ecommerceCS.utils.ConnectionHandler;

@WebServlet("/UpdateCart")
@MultipartConfig
public class UpdateCart extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection connection = null;

	public UpdateCart() {
		super();
	}

	public void init() throws ServletException {
		connection = ConnectionHandler.getConnection(getServletContext());
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		HttpSession s = request.getSession();
		
		// Check params are present and correct
		Integer productID = null;
		Integer supplierID = null;
		Integer quantity = null;
		try {
			productID = Integer.parseInt(request.getParameter("productid"));
			supplierID = Integer.parseInt(request.getParameter("supplierid"));
			quantity = Integer.parseInt(request.getParameter("quantity"));
			
		} catch (NumberFormatException | NullPointerException e) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Incorrect or missing param values");
			return;
		}
		
		if (quantity > 0){
			// SuppliersID and associated ProductsIDs
			Map<Integer, List<Integer>> productsInCart = (LinkedHashMap<Integer, List<Integer>>) s.getAttribute("productsInCart");
			
			if(productsInCart == null) {
				productsInCart = new LinkedHashMap<Integer, List<Integer>>();
			}
			
			List<Integer> productsOfSupplier = productsInCart.get(supplierID);
			
			if(productsOfSupplier == null) {
				productsOfSupplier = new ArrayList<Integer>();
			}
			
			for(int i = 0; i<quantity; i++) {
				productsOfSupplier.add(productID);
			}
			
			productsInCart.put(supplierID, productsOfSupplier);
			
			s.setAttribute("productsInCart", productsInCart);
			
			
			// Map of suppliers and all details to show (0 -> Products, 1 -> Products Price,
			// 2 -> Shipping Price)
			Map<Supplier, List<Object>> productsInCartCompleted = null;

			if (productsInCart != null) {
				productsInCartCompleted = new LinkedHashMap<Supplier, List<Object>>();
				Set<Integer> suppliersID = productsInCart.keySet();

				SupplierDAO supplierDAO = new SupplierDAO(connection);
				ProductDAO productDAO = new ProductDAO(connection);
				SoldByDAO soldByDAO = new SoldByDAO(connection);
				SpendingRangeDAO spendingRangeDAO = new SpendingRangeDAO(connection);

				for (int sID : suppliersID) {
					try {
						Supplier supplier = supplierDAO.findSupplierByID(sID);

						List<Object> details = new ArrayList<Object>();

						Map<Product, Integer> products = new LinkedHashMap<Product, Integer>();

						List<Integer> productsIDs = productsInCart.get(sID);

						// No product Duplicated (0 -> Products)
						products = productDAO.findProductsByIDs(productsIDs);
						details.add(0, products);

						// 1 -> Products Price
						double orderPrice = soldByDAO.sumOfPrices(productsIDs, sID);
						details.add(1, orderPrice);

						// 2 -> Shipping Price
						if (supplier.getFreeShipping() != null && orderPrice >= supplier.getFreeShipping()) {
							details.add(2, 0);
						} else {
							int numProd = productsIDs.size();
							details.add(2, spendingRangeDAO.shipmentPrice(sID, numProd));
						}

						productsInCartCompleted.put(supplier, details);

					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			// Create a GsonBuilder to enable complex map key serialization
			GsonBuilder gsonBuilder = new GsonBuilder();
			gsonBuilder.enableComplexMapKeySerialization();
			Gson gson = gsonBuilder.create();
			// Create a string containing JSON structure
			String json = gson.toJson(productsInCartCompleted);
			// Redirect to the Home page and add productsInCartCompleted to the parameters
			response.setContentType("application/json");
			response.setCharacterEncoding("UTF-8");
			response.getWriter().write(json);
		}
	
	}

	public void destroy() {
		try {
			ConnectionHandler.closeConnection(connection);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

}
