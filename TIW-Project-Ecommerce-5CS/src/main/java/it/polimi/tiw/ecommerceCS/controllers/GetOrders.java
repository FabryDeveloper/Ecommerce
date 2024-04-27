package it.polimi.tiw.ecommerceCS.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
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

import it.polimi.tiw.ecommerceCS.beans.Order;
import it.polimi.tiw.ecommerceCS.beans.User;
import it.polimi.tiw.ecommerceCS.dao.OrderDAO;
import it.polimi.tiw.ecommerceCS.utils.ConnectionHandler;

@WebServlet("/GetOrders")
@MultipartConfig
public class GetOrders extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection connection = null;

	public GetOrders() {
		super();
	}

	public void init() throws ServletException {
		connection = ConnectionHandler.getConnection(getServletContext());
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		HttpSession s = request.getSession();
		User u = (User) s.getAttribute("user");
		
		OrderDAO orderDAO = new OrderDAO(connection);
		
		// Order and associated (0)ProductList, (1)SupplierName and (2)UserAddress
		Map<Order, List<Object>> orders = new LinkedHashMap<Order, List<Object>>();

		try {
			orders = orderDAO.findOrdersForUser(u.getEmail());
		} catch (SQLException e) {
			e.printStackTrace();
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Not possible to recover orders for user");
			return;
		}

		// Create a GsonBuilder to enable complex map key serialization
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.enableComplexMapKeySerialization();
		Gson gson = gsonBuilder.create();
		// Create a string containing JSON structure
		String json = gson.toJson(orders);
		// Redirect to the Home page and add productsInCartCompleted to the parameters
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write(json);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

	public void destroy() {
		try {
			ConnectionHandler.closeConnection(connection);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

}
