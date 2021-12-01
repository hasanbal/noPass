chrome.tabs.query({'active': true, 'lastFocusedWindow': true}, function (tabs) {
	window.curUrl= tabs[0].url;

	var ind = window.curUrl.indexOf(".com");
	window.curUrl = window.curUrl.substr(0,ind+4);

	setTimeout(function() {
		loginFunction();
	}, 100);
});

function makeid(length) {
   var result           = '';
   var characters       = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
   var charactersLength = characters.length;
   for ( var i = 0; i < length; i++ ) {
      result += characters.charAt(Math.floor(Math.random() * charactersLength));
   }
   return result;
}


function makeRequest(url, randomText2){
	const req = new XMLHttpRequest();
    const baseUrl = "http://172.104.242.180/stca/genperm/";
    const urlParams = {"login_uri":url,"pair_key":randomText2};

    req.open("POST", baseUrl, true);
    req.setRequestHeader("Content-type", "application/json");
    req.send(JSON.stringify(urlParams));

	document.getElementsByTagName('body')[0].classList.add('loading');

    req.onreadystatechange = function() { // Call a function when the state changes.
    	console.log("post"+this);
        if (this.readyState === XMLHttpRequest.DONE && this.status === 200) {
            console.log("posttan 200 geldi");
        }
	}
}

function writeScript(url, result){
	var chromeScript = "";
	var email = result.email;
	var password = result.password;

	if(url.search("facebook") != -1){
		chromeScript = "document.getElementById('email').value='"+email+"';";

		chromeScript += "document.getElementById('pass').value='"+password+"';";

		chromeScript += "document.querySelector('input[type=\"submit\"]').click();";

	}
	else if(url.search("twitter") != -1) {
		chromeScript = "document.querySelector('.signin-wrapper input[name=\"session\\[username_or_email\\]\"]').value='"+email+"';";

		chromeScript += "document.querySelector('.signin-wrapper input[name=\"session\\[password\\]\"]').value='"+password+"';";

		chromeScript += "document.querySelector('.signin-wrapper button[type=\"submit\"]').click();";
	}

	console.log(chromeScript);
	return chromeScript;
}

function loginFunction() {
	var randomText2 = makeid(16);

	var randomText = window.curUrl.substr(0,50) + ";" + randomText2;

	var qrcode = new QRCode(document.getElementById("qrcode"), {
		width : 200,
		height : 200,
		colorDark : "#bf3a38",
	});
	qrcode.makeCode(randomText);

	makeRequest(window.curUrl.substr(0,50), randomText2);

    window.setInterval(function() {

    	const req = new XMLHttpRequest();
	    const baseUrl = "http://172.104.242.180/stca/getpass/";

	    const urlParams = {"login_uri":window.curUrl,"pair_key":randomText2};

	    req.open("POST", baseUrl, true);
	    req.setRequestHeader("Content-type", "application/json");
	    req.send(JSON.stringify(urlParams));

	    req.onreadystatechange = function() { // Call a function when the state changes.
	        if (this.readyState === XMLHttpRequest.DONE && this.status === 200) {
	            console.log("getten 200 geldi");
	            console.log(this.response);
	            var res = JSON.parse(this.response);
				var result = {"email":res.first_id, "password":res.timed_pass};

				var chromeScript = writeScript(window.curUrl,result);


			    chrome.tabs.executeScript({
			    	code : chromeScript
			    });
			    window.close();
	        }
	    }

    }, 1000);

};