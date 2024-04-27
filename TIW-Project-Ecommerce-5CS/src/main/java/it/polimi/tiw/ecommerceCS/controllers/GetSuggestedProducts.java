package it.polimi.tiw.ecommerceCS.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.google.gson.Gson;

import it.polimi.tiw.ecommerceCS.beans.Product;
import it.polimi.tiw.ecommerceCS.beans.User;
import it.polimi.tiw.ecommerceCS.dao.ProductDAO;
import it.polimi.tiw.ecommerceCS.utils.ConnectionHandler;

@WebServlet("/GetSuggestedProducts")
@MultipartConfig
public class GetSuggestedProducts extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection connection = null;

	public GetSuggestedProducts() {
		super();
	}

	public void init() throws ServletException {
		connection = ConnectionHandler.getConnection(getServletContext());
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		HttpSession s = request.getSession();
		User u = (User) s.getAttribute("user");

		ProductDAO productDAO = new ProductDAO(connection);
		List<Product> products;

		try {
			products = productDAO.fiveProductsForUser(u.getEmail());
		} catch (SQLException e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,"Not possible to recover products for user");
			return;
		}

		// Redirect to the Home page and add products to the parameters
		String json = new Gson().toJson(products);
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write(json);

	}
	
	public void destroy() {
		try {
			ConnectionHandler.closeConnection(connection);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

}
