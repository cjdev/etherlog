var require = {
	    baseUrl: "/scripts/",
	    paths: {
	    	jquery: "jquery-1.9.1.min",
	    	jqueryui: "jquery-ui-1.10.1.custom.min",
	    	d3:"d3.v3.min"
	    },
	    shim: {

	    	jqueryui: {
	    		deps: ["jquery"]
	        },
	    	jquery: {
	            exports: "jQuery"
	        },
	        d3 : {
	        	exports: "d3"
	        }
	    }
	};