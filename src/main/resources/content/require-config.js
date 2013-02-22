var require = {
	    baseUrl: "/scripts/",
	    paths: {
	    	jquery: "jquery-1.9.1.min",
	    	jqueryui: "jquery-ui-1.10.1.custom.min"
	    },
	    shim: {

	    	jqueryui: {
	    		deps: ["jquery"]
	        },
	    	jquery: {
	            exports: "jQuery"
	        }
	    }
	};