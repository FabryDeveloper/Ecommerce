package it.polimi.tiw.ecommerce.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ArrayList;
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

import it.polimi.tiw.ecommerce.utils.ConnectionHandler;

@WebServlet("/UpdateCart")
public class UpdateCart extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private TemplateEngine templateEngine;
	private Connection connection = null;

	public UpdateCart() {
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
			
			// Return view Cart
			String ctxpath = getServletContext().getContextPath();
			String path = ctxpath + "/GoToCart";
			response.sendRedirect(path);
		} else {
			response.sendRedirect(request.getHeader("referer"));
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
