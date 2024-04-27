package it.polimi.tiw.ecommerce.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
import it.polimi.tiw.ecommerce.dao.ProductDAO;
import it.polimi.tiw.ecommerce.dao.SoldByDAO;
import it.polimi.tiw.ecommerce.dao.SpendingRangeDAO;
import it.polimi.tiw.ecommerce.dao.SupplierDAO;
import it.polimi.tiw.ecommerce.utils.ConnectionHandler;

@WebServlet("/GoToCart")
public class GoToCart extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private TemplateEngine templateEngine;
	private Connection connection = null;

	public GoToCart() {
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
		
		Map<Integer, List<Integer>> productsInCart = (LinkedHashMap<Integer, List<Integer>>) s
				.getAttribute("productsInCart");
		
		// Map of suppliers and all details to show (0 -> Products, 1 -> Products Price, 2 -> Shipping Price)
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

		// Redirect to the Cart page and add productsInCartCompleted to the parameters
		String path = "/WEB-INF/Cart.html";
		ServletContext servletContext = getServletContext();
		final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
		ctx.setVariable("productsInCart", productsInCartCompleted);
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
