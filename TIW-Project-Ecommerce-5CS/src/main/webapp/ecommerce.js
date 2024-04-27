{ // avoid variables ending up in the global scope

	// page components
	let suggestedProducts, menu, listResearch, productDetails, floatingWindow, listCart, listOrders, productsInCart
	pageOrchestrator = new PageOrchestrator(); // main controller

	window.addEventListener("load", () => {
		if (sessionStorage.getItem("username") == null) {
			window.location.href = "index.html";
		} else {
			pageOrchestrator.start(); // initialize the components
			pageOrchestrator.refresh();
		} // display initial content
	}, false);


	// Constructors of view components

	function Message(_completeMessage, messagecontainer) {
		this.completeMessage = _completeMessage;
		this.show = function() {
			if (this.completeMessage != null) {
				messagecontainer.textContent = this.completeMessage;
			}
			messagecontainer.closest("div").style.display = "";
		}
		this.reset = function() {
			messagecontainer.closest("div").style.display = "none";
		}
	}

	function SuggestedProducts(_alert, _suggestedConatiner) {
		this.alert = _alert;
		this.suggestedContainer = _suggestedConatiner;

		this.reset = function() {
			this.suggestedContainer.style.visibility = "hidden";
			this.suggestedContainer.style.maxHeight = "0";
		}

		this.show = function() {
			var self = this;
			makeCall("GET", "GetSuggestedProducts", null,
				function(req) {
					if (req.readyState == 4) {
						var message = req.responseText;
						if (req.status == 200) {
							var productsToShow = JSON.parse(req.responseText);
							self.update(productsToShow); // self visible by closure
						} else if (req.status == 403) {
							window.location.href = req.getResponseHeader("Location");
							window.sessionStorage.removeItem('username');
						}
						else {
							window.alert(message);
						}
					}
				}
			);
		};


		this.update = function(arrayProducts) {
			var table, head, body, row, destcell, linkcell, anchor, img;
			this.suggestedContainer.innerHTML = ""; // empty the table body
			if (arrayProducts.length == 0) {
				this.alert.textContent = "No products on offer";
				this.alert.style.visibility = "visible";
				this.alert.style.maxHeight = "none";
				return;
			}
			// build updated list
			table = document.createElement("table");
			this.suggestedContainer.appendChild(table);
			head = document.createElement("thead");
			table.appendChild(head);
			row = document.createElement("tr");
			head.appendChild(row);
			arrayProducts.forEach(function(product) { // self visible here, not this
				// Category
				destcell = document.createElement("th");
				destcell.textContent = product.category;
				destcell.className = "category";
				row.appendChild(destcell);
			});
			row = document.createElement("tr");
			head.appendChild(row);
			arrayProducts.forEach(function(product) { // self visible here, not this
				// Immage
				destcell = document.createElement("th");
				img = document.createElement("img");
				img.setAttribute('src', "data:image/jpeg;base64," + product.photo);
				img.setAttribute('alt', "Immagine");
				destcell.appendChild(img);
				row.appendChild(destcell);
			});
			body = document.createElement("tbody");
			table.appendChild(body);
			row = document.createElement("tr");
			body.appendChild(row);
			arrayProducts.forEach(function(product) { // self visible here, not this
				// Name (with anchor)
				linkcell = document.createElement("td");
				anchor = document.createElement("a");
				linkcell.appendChild(anchor);
				linkText = document.createTextNode(product.name);
				anchor.appendChild(linkText);
				anchor.setAttribute('productName', product.name); // set a custom HTML attribute
				anchor.addEventListener("click", (e) => {
					// dependency via module parameter
					var keyword = e.target.closest("a").getAttribute('productName');
					makeCall("GET", 'GetResearch?keyword=' + encodeURIComponent(keyword), null,
						function(req) {
							if (req.readyState == 4) {
								var message = req.responseText;
								if (req.status == 200) {
									pageOrchestrator.refresh("research", keyword, JSON.parse(message));
								} else if (req.status == 403) {
									window.location.href = req.getResponseHeader("Location");
									window.sessionStorage.removeItem('username');
								}
								else {
									window.alert(message);
								}
							}
						}
					);
				}); // the list must know the details container
				anchor.href = "#";
				row.appendChild(linkcell);
			});
			row = document.createElement("tr");
			body.appendChild(row);
			arrayProducts.forEach(function(product) { // self visible here, not this
				// Description
				destcell = document.createElement("td");
				destcell.textContent = product.description;
				row.appendChild(destcell);
			});

			this.suggestedContainer.style.visibility = "visible";
			this.suggestedContainer.style.maxHeight = "none";
		}

	}

	function Menu(menuContainer) {
		this.menuContainer = menuContainer;
		this.registerEvents = function(orchestrator) {
			this.menuContainer.querySelectorAll("a").forEach(function(page) {
				page.addEventListener('click', (e) => {
					var currentPage = e.target.closest("a").id;
					orchestrator.refresh(currentPage);
				});

			});

			this.menuContainer.querySelector("button").addEventListener('click', (event) => {
				event.preventDefault(); // Prevent default send
				var form = event.target.closest("form");
				if (form.checkValidity()) {
					var keyWord = form.querySelector("input").value;
					makeCall("GET", 'GetResearch?keyword=' + encodeURIComponent(keyWord), null,
						function(req) {
							if (req.readyState == 4) {
								var message = req.responseText;
								if (req.status == 200) {
									pageOrchestrator.refresh("research", keyWord, JSON.parse(message));
								} else if (req.status == 403) {
									window.location.href = req.getResponseHeader("Location");
									window.sessionStorage.removeItem('username');
								}
								else {
									window.alert(message);
								}
							}
						}
					);
				} else {
					form.reportValidity();
				}
			});
		}

		this.show = function(currentPage) {
			this.menuContainer.querySelectorAll("a").forEach(function(page) {
				page.className = "";
			});
			if (document.getElementById(currentPage) != null) {
				document.getElementById(currentPage).className = "active";
			}
			this.menuContainer.querySelector("form").reset();
		}
	}

	function ListResearch(_alert, _searchedContainer) {
		this.alert = _alert;
		this.searchedContainer = _searchedContainer;

		this.reset = function() {
			// Hide all Research tables
			document.getElementById("researchDiv").style.visibility = "hidden";
			document.getElementById("researchDiv").style.maxHeight = "0";
		}

		this.show = function(keyWord, data) {
			var img, body, row, destcell, linkcell, anchor;
			this.searchedContainer.innerHTML = ""; // empty the table body
			this.products = JSON.parse(data.products);

			productDetails.reset();
			// build updated list
			if (Object.keys(this.products).length === 0) {
				this.alert.textContent = "No products found";
				this.alert.style.visibility = "visible";
				this.alert.style.maxHeight = "none";
				return;
			}
			body = this.searchedContainer;
			this.products.forEach(function(product) { // self visible here, not this
				// product[0] = product, product[1] = product price
				row = document.createElement("tr");
				body.appendChild(row);
				// Immage
				destcell = document.createElement("td");
				img = document.createElement("img");
				img.setAttribute('src', "data:image/jpeg;base64," + product[0].photo);
				img.setAttribute('alt', "Immagine");
				destcell.appendChild(img);
				destcell.className = "productImage";
				row.appendChild(destcell);

				// Id
				destcell = document.createElement("td");
				destcell.textContent = product[0].id;
				row.appendChild(destcell);

				// Name (with anchor)
				linkcell = document.createElement("td");
				anchor = document.createElement("a");
				linkcell.appendChild(anchor);
				linkText = document.createTextNode(product[0].name);
				anchor.appendChild(linkText);
				anchor.setAttribute('productId', product[0].id); // set a custom HTML attribute
				anchor.addEventListener("click", (e) => {
					// dependency via module parameter
					var productId = e.target.closest("a").getAttribute('productId');
					productDetails.show(keyWord, productId);
				}); // the list must know the details container
				anchor.href = "#";
				row.appendChild(linkcell);

				// Price
				destcell = document.createElement("td");
				destcell.textContent = product[1].toLocaleString('it-IT', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + "€";
				row.appendChild(destcell);

			});
			document.getElementById("researchDiv").style.visibility = "visible";
			document.getElementById("researchDiv").maxHeight = "none";
		}

		this.update = function(data) {
			this.showProduct = JSON.parse(data.showProduct);
			this.products = JSON.parse(data.products);
			var self = this;


			body = this.searchedContainer;
			rows = body.childNodes;
			i = 0;
			this.products.forEach(function(product) {
				rows[i].className = "";
				if (self.showProduct != null && self.showProduct.id == product[0].id) {
					rows[i].className = "current";
				}

				i++;
			})
		}
	}

	function ProductDetails(_detailsContainer) {
		this.detailsContainer = _detailsContainer;
		this.suppliersContainer = document.getElementById("id_suppliers");
		this.alertSuppliers = new Message(null, document.getElementById("id_alertSuppliers"));
		this.alertNoProduct = new Message(null, document.getElementById("id_alertNoProduct"));

		this.reset = function() {
			this.detailsContainer.closest("div").style.visibility = "hidden";
			this.detailsContainer.closest("div").style.maxHeigth = "0";
			this.detailsContainer.style.visibility = "hidden";
			this.detailsContainer.style.maxHeight = "0";
			this.alertSuppliers.reset();
			this.alertNoProduct.reset();
		}

		this.show = function(keyWord, productId) {
			var self = this;
			makeCall("GET", 'GetResearch?keyword=' + encodeURIComponent(keyWord) + '&productid=' + encodeURIComponent(productId), null,
				function(req) {
					if (req.readyState == 4) {
						var message = req.responseText;
						if (req.status == 200) {
							self.update(JSON.parse(message));
						} else if (req.status == 403) {
							window.location.href = req.getResponseHeader("Location");
							window.sessionStorage.removeItem('username');
						}
						else {
							window.alert(message);
						}
					}
				}
			);
		}

		this.update = function(productDetails) {
			var img, body, row, destcell, formcell, input, button, spancell, listcell;
			this.detailsContainer.innerHTML = ""; // empty the table body
			this.suppliersContainer.innerHTML = "";
			this.showProduct = JSON.parse(productDetails.showProduct);
			this.showProductPrice = JSON.parse(productDetails.showProductPrice).toLocaleString('it-IT', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
			this.showSuppliers = JSON.parse(productDetails.showSuppliers);
			var self = this;
			if (this.showProduct === null) {
				this.alertNoProduct.show();
				this.detailsContainer.closest("div").style.visibility = "hidden";
				this.detailsContainer.closest("div").style.maxHeigth = "0";
			}

			// update row of listResearch with color
			listResearch.update(productDetails);
			// build updated Product Details
			body = this.detailsContainer;
			row = document.createElement("tr");
			body.appendChild(row);
			// Immage
			destcell = document.createElement("td");
			img = document.createElement("img");
			img.setAttribute('src', "data:image/jpeg;base64," + this.showProduct.photo);
			img.setAttribute('alt', "Immagine");
			destcell.appendChild(img);
			destcell.className = "productImage";
			row.appendChild(destcell);

			// Id
			destcell = document.createElement("td");
			destcell.textContent = this.showProduct.id;
			row.appendChild(destcell);

			// Name (with anchor)
			destcell = document.createElement("td");
			destcell.textContent = this.showProduct.name;
			row.appendChild(destcell);

			// Price
			destcell = document.createElement("td");
			destcell.textContent = this.showProductPrice.toLocaleString('it-IT', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + "€";
			row.appendChild(destcell);

			// Category
			destcell = document.createElement("td");
			spancell = document.createElement("span");
			spancell.className = "category";
			spancell.textContent = this.showProduct.category;
			destcell.appendChild(spancell);
			row.appendChild(destcell);

			// Description
			destcell = document.createElement("td");
			destcell.textContent = this.showProduct.description;
			row.appendChild(destcell);

			this.detailsContainer.closest("div").style.visibility = "visible";
			this.detailsContainer.closest("div").style.maxHeight = "none";
			this.detailsContainer.style.visibility = "visible";
			this.detailsContainer.style.maxHeight = "none";

			// build updated Product Details

			if (Object.keys(this.showSuppliers).length === 0) {
				this.alertSuppliers.completeMessage = this.showProduct.name;
				this.alertSuppliers.show();
				this.suppliersContainer.closest("table").style.visibility = "hidden";
				this.suppliersContainer.closest("table").style.maxHeigth = "0";
			}

			body = this.suppliersContainer;

			this.showSuppliers.forEach(function(supplier) { // self visible here, not this
				// supplier[0] = supplier, supplier[1][0] = product price, supplier[1][1] = spending ranges
				// supplier[1][2] = # Products, supplier[1][3] = Products price

				row = document.createElement("tr");
				body.appendChild(row);

				// Name
				destcell = document.createElement("td");
				destcell.textContent = supplier[0].name;
				row.appendChild(destcell);

				// Rating
				destcell = document.createElement("td");
				destcell.className = "yellow-star";
				destcell.textContent = "";
				for (i = 0; i < supplier[0].rating; i++) {
					destcell.textContent += "\u2605";
				}
				row.appendChild(destcell);

				// Price
				destcell = document.createElement("td");
				destcell.textContent = supplier[1][0].toLocaleString('it-IT', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + "€";
				row.appendChild(destcell);

				// Spending Ranges
				destcell = document.createElement("td");
				destcell.innerHTML = "";
				if (supplier[1][1].length == 0) {
					destcell = document.createElement("td");
					destcell.innerHTML = "No spending<br>ranges";
				}

				supplier[1][1].forEach(function(range) {
					destcell.innerHTML += range.min;
					if (range.max == 0) {
						destcell.innerHTML += ">";
					} else {
						destcell.innerHTML += " - " + range.max;
					}
					destcell.innerHTML += " (" + range.price.toLocaleString('it-IT', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + "€" + ")";
					destcell.innerHTML += "<br>";
				})
				row.appendChild(destcell);

				// Free shipping
				destcell = document.createElement("td");
				if (supplier[0].freeShipping == null) {
					destcell.textContent = "No Free Shipping";
				} else {
					destcell.textContent = "From " + supplier[0].freeShipping.toLocaleString('it-IT', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + "€";
				}
				row.appendChild(destcell);

				// # Products
				destcell = document.createElement("td");
				destcell.style.maxHeigth = "none";
				row.appendChild(destcell);
				spancell = document.createElement("span");
				destcell.appendChild(spancell);

				spancell.innerHTML = "";
				if (supplier[1].length < 3) {
					spancell.innerHTML = "No other<br>products";
				} else {
					spancell.className = "num-products";
					spancell.textContent = supplier[1][2];

					// mouse hover: recover data and show window
					spancell.addEventListener('mouseover', (event) => {
						floatingWindow.show(supplier);
						floatingWindow.update(event);
					});

					// mouse move: show window (data already saved in the window)
					spancell.addEventListener('mousemove', (event) => {
						floatingWindow.update(event);
					});

					// mouse out: hide window and delete data
					spancell.addEventListener('mouseout', (event) => {
						floatingWindow.reset();
					});

				}
				// Products Price
				destcell = document.createElement("td");
				if (supplier[1].length < 4) {
					destcell.textContent = "0€";
				} else {
					destcell.textContent = supplier[1][3].toLocaleString('it-IT', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + "€";
				}
				row.appendChild(destcell);

				// Cart Form
				destcell = document.createElement("td");
				destcell.className = "button-cart";
				formcell = document.createElement("form");
				formcell.action = "#";
				destcell.appendChild(formcell);

				input = document.createElement("input");
				input.type = "number";
				input.placeholder = "Quantity..";
				input.name = "quantity";
				input.min = 0;
				input.required = true;
				formcell.appendChild(input);

				input = document.createElement("input");
				input.type = "hidden";
				input.name = "productid";
				input.value = self.showProduct.id;
				formcell.appendChild(input);

				input = document.createElement("input");
				input.type = "hidden";
				input.name = "supplierid";
				input.value = supplier[0].id;
				formcell.appendChild(input);

				button = document.createElement("button");
				button.type = "submit";
				button.name = "submit";
				button.textContent = "\u{1F6D2}";
				formcell.appendChild(button);

				button.addEventListener('click', (event) => {
					event.preventDefault(); // Prevent default send
					var form = event.target.closest("form");
					if (form.checkValidity()) {
						var productId = form.querySelector("input[name=productid]").value;
						var supplierId = form.querySelector("input[name=supplierid]").value;
						var quantity = form.querySelector("input[name=quantity]").value;
						makeCall("POST", 'UpdateCart?productid=' + encodeURIComponent(productId)
							+ "&supplierid=" + encodeURIComponent(supplierId) + "&quantity=" + encodeURIComponent(quantity),
							null, function(req) {
								if (req.readyState == 4) {
									var message = req.responseText;
									if (req.status == 200) {
										// In session is needed the JSON stringyfied
										sessionStorage.setItem('productsInCart', message);

										pageOrchestrator.refresh("cart");

									} else if (req.status == 403) {
										window.location.href = req.getResponseHeader("Location");
										window.sessionStorage.removeItem('username');
									}
									else {
										window.alert(message);
									}
								}
							}
						);
					} else {
						form.reportValidity();
					}
				});

				row.appendChild(destcell);

			});
		}
	}

	function FLoatingWindow(_floatingContainer) {
		this.floatContainer = _floatingContainer;
		this.reset = function() {
			this.floatContainer.style.display = 'none';
			this.floatContainer.style.zIndex = '-1';
			this.floatContainer.innerHTML = "";
		}

		this.show = function(supplier) {
			//  Find Products
			productsInCart = JSON.parse(sessionStorage.getItem('productsInCart'));

			// Trovo i prodotti del supplier controllando che l'id sia lo stesso e poi restituendo
			// l'array di prodotti
			let productsOfSupplier = productsInCart.find((s) => s[0].id == supplier[0].id)[1][0];
			
			// Putting data inside the window
			destcell = document.createElement("ul");
			destcell.className = "floating-list";
			this.floatContainer.appendChild(destcell);

			productsOfSupplier.forEach(function(p) {

				listcell = document.createElement("li");
				destcell.appendChild(listcell);


				spancell = document.createElement("span");
				listcell.appendChild(spancell);
				spancell.textContent = "";
				if (p[1] > 1) {
					spancell.textContent += "[" + p[1] + "] ";
				}
				spancell.textContent += p[0].name + " (ID: " + p[0].id + ")";
			})
		}


		this.update = function(event) {

			// Showing the window
			this.floatContainer.style.display = 'block';
			this.floatContainer.style.zIndex = '1';

			// Posiziona la finestra sovrapposta in base alla posizione del nodo
			var mouseX = event.clientX;
			var mouseY = event.clientY;
			var dist = 20;
			var windowX = mouseX - this.floatContainer.offsetWidth - dist;
			var windowY = mouseY - this.floatContainer.offsetHeight - dist;
			this.floatContainer.style.left = windowX + 'px';
			this.floatContainer.style.top = windowY + 'px';
		}

	}

	function ListCart(_alert, _cartContainer) {
		this.cartContainer = _cartContainer;
		this.alert = _alert;

		this.reset = function() {
			// Hide Cart List
			document.getElementById("id_cartDiv").style.visibility = "hidden";
			document.getElementById("id_cartDiv").style.maxHeight = "0";
		}

		this.show = function() {
			productsInCart = JSON.parse(sessionStorage.getItem('productsInCart'));
			this.update(productsInCart);
		}

		this.update = function(pInCart) {
			var img, body, row, destcell, formcell, input, button, spancell, listcell;
			this.cartContainer.innerHTML = ""; // empty the table body

			this.reset();
			if (pInCart === null || pInCart.length == 0) {
				this.alert.textContent = "No products for any supplier";
				this.alert.style.visibility = "visible";
				this.alert.style.maxHeight = "none";
				return;
			}

			// build updated List Cart
			// supplier[0] = supplier, supplier[1][0] = products, 
			// supplier[1][1] = products price, supplier[1][2] = shipping price
			body = this.cartContainer;
			pInCart.forEach(function(supplier) {
				row = document.createElement("tr");
				body.appendChild(row);

				// Name
				destcell = document.createElement("td");
				destcell.textContent = supplier[0].name;
				destcell.className = "supplier-name";
				row.appendChild(destcell);

				// Products
				destcell = document.createElement("td");
				row.appendChild(destcell);
				listcell = document.createElement("ul");
				listcell.className = "products";
				destcell.appendChild(listcell);

				productsOfSupplier = supplier[1][0];
				// build list of products
				// p[0] = product, p[1] = quantity
				productsOfSupplier.forEach(function(p) {

					destcell = document.createElement("li");
					listcell.appendChild(destcell);


					spancell = document.createElement("span");
					destcell.appendChild(spancell);
					spancell.textContent = "";
					if (p[1] > 1) {
						spancell.textContent += "[" + p[1] + "] ";
					}
					spancell.textContent += p[0].name + " (ID: " + p[0].id + ")";

					img = document.createElement("img");
					destcell.appendChild(img);
					img.setAttribute('src', "data:image/jpeg;base64," + p[0].photo);
					img.setAttribute('alt', "Immagine");
					img.className = "productImage";
				})

				// Products Price
				destcell = document.createElement("td");
				destcell.textContent = supplier[1][1].toLocaleString('it-IT', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + "€";
				row.appendChild(destcell);

				// Shipping Price
				destcell = document.createElement("td");
				destcell.textContent = supplier[1][2].toLocaleString('it-IT', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + "€";
				row.appendChild(destcell);

				// Order Button
				destcell = document.createElement("td");
				destcell.className = "button-order";
				formcell = document.createElement("form");
				formcell.action = "#";
				destcell.appendChild(formcell);

				input = document.createElement("input");
				input.type = "hidden";
				input.name = "supplierid";
				input.value = supplier[0].id;
				formcell.appendChild(input);

				button = document.createElement("button");
				button.type = "submit";
				button.name = "submit";
				button.textContent = "Order";
				formcell.appendChild(button);

				button.addEventListener('click', (event) => {
					event.preventDefault(); // Prevent Default send
					var supplierId = formcell.querySelector("input[name=supplierid]").value;
					makeCall("POST", 'CreateOrder?supplierid=' + encodeURIComponent(supplierId), null,
						function(req) {
							if (req.readyState == 4) {
								var message = req.responseText;
								if (req.status == 200) {

									// Parsing the Json stringyfied in storage
									productsInCart = JSON.parse(sessionStorage.getItem('productsInCart'));

									//Removing supplier from the array
									productsInCart = productsInCart.filter((s) => s[0].id != supplierId);
									
									// In session is needed the JSON stringyfied
									sessionStorage.setItem('productsInCart', JSON.stringify(productsInCart));

									pageOrchestrator.refresh("orders");

								} else if (req.status == 403) {
									window.location.href = req.getResponseHeader("Location");
									window.sessionStorage.removeItem('username');
								}
								else {
									window.alert(message);
								}
							}
						}
					);
				});

				row.appendChild(destcell);

			});

			document.getElementById("id_cartDiv").style.visibility = "visible";
			document.getElementById("id_cartDiv").style.maxHeight = "none";

		}
	}

	function ListOrders(_alert, _ordersContainer) {
		this.ordersContainer = _ordersContainer;
		this.alert = _alert;

		this.reset = function() {
			// Hide Cart List
			document.getElementById("id_ordersDiv").style.visibility = "hidden";
			document.getElementById("id_ordersDiv").style.maxHeight = "0";
		}

		this.show = function() {
			var self = this;
			makeCall("GET", 'GetOrders', null,
				function(req) {
					if (req.readyState == 4) {
						var message = req.responseText;
						if (req.status == 200) {
							self.update(JSON.parse(message));
						} else if (req.status == 403) {
							window.location.href = req.getResponseHeader("Location");
							window.sessionStorage.removeItem('username');
						}
						else {
							window.alert(message);
						}
					}
				}
			);
			
		}

		this.update = function(orders) {
			var img, body, row, destcell, anchor, spancell, listcell;
			this.ordersContainer.innerHTML = ""; // empty the table body

			this.reset();
			if (Object.keys(orders).length === 0) {
				this.alert.textContent = "You have no orders";
				this.alert.appendChild(document.createElement("br"));
				anchor = document.createElement("a");
				anchor.textContent = "Go Shopping!";
				anchor.addEventListener("click", (e) => {
					// dependency via module parameter
					pageOrchestrator.refresh("home");
				}); // the list must know the details container
				anchor.href = "#";
				this.alert.appendChild(anchor);
				this.alert.appendChild
				this.alert.style.visibility = "visible";
				this.alert.style.maxHeight = "none";
				return;
			}

			// build updated List Orders
			// orders[x] = order
			// order[0] = order, order[1][0] = products, order[1][1] = name supplier, order[1][2] = address user
			body = this.ordersContainer;
			orders.forEach(function(order) {
				row = document.createElement("tr");
				body.appendChild(row);

				// ID
				destcell = document.createElement("td");
				destcell.textContent = order[0].id;
				destcell.className = "order-id";
				row.appendChild(destcell);

				// Supplier name
				destcell = document.createElement("td");
				destcell.textContent = order[1][1];
				row.appendChild(destcell);
				
				// Products List
				destcell = document.createElement("td");
				row.appendChild(destcell);
				listcell = document.createElement("ul");
				listcell.className = "products";
				destcell.appendChild(listcell);

				productsOfOrder = order[1][0];
				// build list of products
				// p[0] = product, p[1] = quantity
				productsOfOrder.forEach(function(p) {

					destcell = document.createElement("li");
					listcell.appendChild(destcell);


					spancell = document.createElement("span");
					destcell.appendChild(spancell);
					spancell.textContent = "";
					if (p[1] > 1) {
						spancell.textContent += "[" + p[1] + "] ";
					}
					spancell.textContent += p[0].name + " (ID: " + p[0].id + ")";

					img = document.createElement("img");
					destcell.appendChild(img);
					img.setAttribute('src', "data:image/jpeg;base64," + p[0].photo);
					img.setAttribute('alt', "Immagine");
					img.className = "productImage";
				})

				// Total Price
				destcell = document.createElement("td");
				destcell.textContent = order[0].price.toLocaleString('it-IT', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + "€";
				row.appendChild(destcell);

				// Shipping Date
				destcell = document.createElement("td");
				destcell.textContent = order[0].shipment;
				row.appendChild(destcell);
				
				// Shipping Address
				destcell = document.createElement("td");
				destcell.textContent = order[1][2];
				row.appendChild(destcell);

			});

			document.getElementById("id_ordersDiv").style.visibility = "visible";
			document.getElementById("id_ordersDiv").style.maxHeight = "none";

		}
	}


	function PageOrchestrator() {
		var alertContainer = document.getElementById("id_alert");
		alertContainer.style.visibility = "hidden";
		alertContainer.style.maxHeight = "0";


		this.start = function() {
			// Creating view elements
			welcomeMessage = new Message(sessionStorage.getItem('username'), document.getElementById("id_welcomeMessage"));
			researchMessage = new Message(null, document.getElementById("id_researchMessage"));
			cartMessage = new Message(null, document.getElementById("id_cartMessage"));
			ordersMessage = new Message(null, document.getElementById("id_ordersMessage"));

			menu = new Menu(document.getElementById("id_menu"));

			suggestedProducts = new SuggestedProducts(
				alertContainer,
				document.getElementById("id_suggestedProducts"));

			listResearch = new ListResearch(alertContainer, document.getElementById("id_researchedProducts"));

			productDetails = new ProductDetails(document.getElementById("id_detailsProduct"));

			floatingWindow = new FLoatingWindow(document.getElementById("id_floatingWindow"));

			listCart = new ListCart(alertContainer, document.getElementById("id_listCart"));

			listOrders = new ListOrders(alertContainer, document.getElementById("id_listOrders"));

			// Registering Events

			menu.registerEvents(this);

			document.querySelector("a[href='Logout']").addEventListener('click', () => {
				window.sessionStorage.removeItem('username');
				window.sessionStorage.removeItem('productsInCart');
			});

		};

		this.refresh = function(currentPage, message, data) {
			// Default behaviour (show home)
			if (currentPage == null) currentPage = "home";
			
			// Hide alert
			alertContainer.textContent = "";
			alertContainer.style.visibility = "hidden";
			alertContainer.style.maxHeight = "0";
			
			// Show menu
			menu.show(currentPage);
			
			// Reset all view components
			welcomeMessage.reset();
			researchMessage.reset();
			cartMessage.reset();
			ordersMessage.reset();
			suggestedProducts.reset();
			listResearch.reset();
			productDetails.reset();
			floatingWindow.reset();
			listCart.reset();
			listOrders.reset();
			
			// Show the rigth view component
			if (currentPage == "home") {
				welcomeMessage.show();
				suggestedProducts.show();
			} else if (currentPage == "research") {
				researchMessage.completeMessage = message;
				researchMessage.show();
				listResearch.show(message, data);
			} else if (currentPage == "cart") {
				cartMessage.show();
				listCart.show()
			} else if (currentPage == "orders") {
				ordersMessage.show();
				listOrders.show();
			}
		};
	}
}