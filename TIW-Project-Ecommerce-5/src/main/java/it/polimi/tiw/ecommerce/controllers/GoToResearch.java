package it.polimi.tiw.ecommerce.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;

import it.polimi.tiw.ecommerce.beans.Product;
import it.polimi.tiw.ecommerce.beans.Supplier;
import it.polimi.tiw.ecommerce.beans.User;
import it.polimi.tiw.ecommerce.dao.ProductDAO;
import it.polimi.tiw.ecommerce.dao.SoldByDAO;
import it.polimi.tiw.ecommerce.dao.SupplierDAO;
import it.polimi.tiw.ecommerce.dao.VisualizationDAO;
import it.polimi.tiw.ecommerce.utils.ConnectionHandler;

@WebServlet("/GoToResearch")
public class GoToResearch extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private TemplateEngine templateEngine;
	private Connection connection = null;

	public GoToResearch() {
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

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		HttpSession s = request.getSession();
		User u = (User) s.getAttribute("user");

		String keyWord = request.getParameter("keyword");

		// Check parameter is present
		if (keyWord == null || keyWord.isEmpty()) {
			// Return view Home
			String ctxpath = getServletContext().getContextPath();
			String path = ctxpath + "/GoToHome";
			response.sendRedirect(path);
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
			if(products.keySet().stream().map(p -> p.getId()).anyMatch(pId -> pId == showProductID)){
				showProduct = products.keySet().stream().filter(p -> p.getId() == showProductID).findFirst().get();

				showProductPrice = products.get(showProduct);

				SupplierDAO suppDAO = new SupplierDAO(connection);
				SoldByDAO soldByDAO = new SoldByDAO(connection);
				VisualizationDAO visDAO = new VisualizationDAO(connection);
				
				Map<Integer, List<Integer>> productsInCart = (LinkedHashMap<Integer, List<Integer>>) s
						.getAttribute("productsInCart");

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
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
					"Not possible to recover this product for this id");
			return;
		}
	

	// Redirect to the Research page and add parameters
	String path = "/WEB-INF/Research.html";
	ServletContext servletContext = getServletContext();
	final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
	ctx.setVariable("products", products);
	ctx.setVariable("showProduct", showProduct);
	ctx.setVariable("showSuppliers", showSuppliers);
	ctx.setVariable("showProductPrice", showProductPrice);
	templateEngine.process(path, ctx, response.getWriter());
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
