define(["jquery", "http", "uuid"], function($, http, uuid){
	 var body = $("body");
	  body.append('<button class="add-backlog-button">Add</button>');
	  body.find(".add-backlog-button").click(function(){
		  var name = prompt("Give the backlog a name","some name");
		  http({
			  url: "/api/backlogs",
	          method: "POST",
	          data:JSON.stringify({
	        	  id:"whatever",
	        	  name:name,
	        	  memo:"Initial Version",
	        	  items:[]
	          }),
	          onResponse: function (response) {
	        	  if(response.status === 201){
	        		  window.location.reload();
	        	  }else{
	        		  alert("ERROR: " + response.status);
	        	  }
	          }
		  });
	  });
	  
	  http({
		  url: "/api/backlogs",
         method: "GET",
         onResponse: function (response) {
       	  var backlogs=JSON.parse(response.body);
       	  $.each(backlogs, function(idx, backlog){
       		  body.append('<div><a href="/backlog/' + backlog.id + '">' + backlog.name + '</a></div>');
       	  });
         }
	  });
});