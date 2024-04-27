package it.polimi.tiw.ecommerce.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;

import it.polimi.tiw.ecommerce.beans.User;
import it.polimi.tiw.ecommerce.dao.OrderDAO;
import it.polimi.tiw.ecommerce.exceptions.BadOrder;
import it.polimi.tiw.ecommerce.utils.ConnectionHandler;

@WebServlet("/CreateOrder")
public class CreateOrder extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private TemplateEngine templateEngine;
	private Connection connection = null;

	public CreateOrder() {
		super();
	}

	public void init() throws ServletException {
		ServletContext servletContext = getServletContext();
		ServletContextTemplateResolver templateResolver = new ServletContextTemplateResolver(servletContext);
		templateResolver.setTemplateMode(TemplateMode.HTML);
		this.templateEngine = new TemplateEngine();
		this.templateEngine.setTemplateResolver(templateResolver);
		templateResolver.setSuffix(".html");
		connection = ConnectionHandler.getConnection(getServletContext());
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		HttpSession s = request.getSession();

		// Check params are present and correct
		Integer supplierID = null;

		try {
			supplierID = Integer.parseInt(request.getParameter("supplierid"));

		} catch (NumberFormatException | NullPointerException e) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Incorrect or missing param values");
			return;
		}

		try {
			Map<Integer, List<Integer>> productsInCart = (LinkedHashMap<Integer, List<Integer>>) s.getAttribute("productsInCart");
			if (productsInCart != null) {
				if (productsInCart.containsKey(supplierID)) {
					User user = (User) s.getAttribute("user");

					OrderDAO orderDAO = new OrderDAO(connection);
					
					orderDAO.createOrder(user.getEmail(), supplierID, productsInCart.get(supplierID));
					
					productsInCart.remove(supplierID);
					// Return view Order
					String ctxpath = getServletContext().getContextPath();
					String path = ctxpath + "/GoToOrders";
					response.sendRedirect(path);
					
				} else {
					throw new BadOrder(
							"Order cannot be done without products of supplier " + supplierID + " in the cart");
				}
			} else {
				throw new BadOrder("Order cannot be done without products in the cart");
			}

		} catch (SQLException e1) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Not possible to create order");
			return;
		} catch (BadOrder e2) {
			response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED, "Not allowed");
			return;
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