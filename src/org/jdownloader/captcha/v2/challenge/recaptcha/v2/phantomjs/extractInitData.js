(function() {
	try {
		var initData = eval(initScript);
		initData = eval(initData);
		if ("multicaptcha" == initData[5][5]) {
			console.log("TYPE multicaptcha");
			ret = {};
			ret.x = initData[5][4][5][0][0][4];
			ret.y = initData[5][4][5][0][0][3];
			ret.challengeType = initData[5][5];
			return ret;
		} else if ("nocaptcha" == initData[5][5]) {
			console.log("TYPE NOCAPTCHA");
			ret = {};
			ret.x = -1;
			ret.y = -1;
			ret.challengeType = initData[5][5];
			return ret;
		}
		console.log("TYPE "+initData[5][4][1][0]);
		ret = {};
		ret.contentType = initData[5][4][1][0];
		ret.x = initData[5][4][1][4];
		ret.y = initData[5][4][1][3];
		ret.challengeType = initData[5][5];
		if (typeof (initData[5][4][1][1]) !== 'undefined') {
			ret.explainUrl = [ 5 ][4][1][1];
		}

		return ret;
	} catch (err) {
		ret = {};
		ret.x = -1;
		ret.y = -1;
		ret.challengeType = "unknown";
		return ret;
	}
	// var Ko = function(a) {
	// switch (a) {
	// case "default":
	// return new mo;
	// case "nocaptcha":
	// return new Eo;
	// case "imageselect":
	// return new so;
	// case "tileselect":
	// return new so;
	// case "dynamic":
	// return new xo;
	// case "audio":
	// return new go;
	// case "text":
	// return new Jo
	// }
	// }
})()