/**
 * Login management
 */

(function() { // avoid variables ending up in the global scope

  document.getElementById("loginButton").addEventListener('click', (e) => {
    var form = e.target.closest("form");
    if (form.checkValidity()) {
      makeCall("POST", 'CheckLogin', e.target.closest("form"),
        function(x) {
          if (x.readyState == XMLHttpRequest.DONE) {
            var message = x.responseText;
            switch (x.status) {
              case 200:
            	sessionStorage.setItem('username', message);
                window.location.href = "HomeCS.html";
                break;
              case 400: // bad request
                document.getElementById("errormessage").textContent = message;
                document.getElementById("errormessage").className = "error";
                break;
              case 401: // unauthorized
                  document.getElementById("errormessage").textContent = message;
                  document.getElementById("errormessage").className = "error";
                  break;
              case 500: // server error
            	document.getElementById("errormessage").textContent = message;
            	document.getElementById("errormessage").className = "error";
                break;
               default: //generic error
               	document.getElementById("errormessage").textContent = message;
               	document.getElementById("errormessage").className = "error";
            }
          }
        }
      );
    } else {
		e.preventDefault(); // needed to work with safari
    	 form.reportValidity();
    }
  });

})();