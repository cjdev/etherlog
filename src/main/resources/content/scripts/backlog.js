define(["jquery", "http"], function($, http){

	  var backlogId = 23;
	  
	  function render(backlog){
		  console.log(backlog);
		  $("#title").text(backlog.name);
		  
		  $.each(backlog.items, function(idx, item){
			  var body = $(".backlog");
			  if(item.kind==="goal"){
				  body.append('<div class="milestone divider clearfix">GOAL: ' + item.name + '</div>');
			  }else if(item.kind==="story"){
				  body.append('<div class="story project-chunk">' + item.name + '</div>');
			  }else if(item.kind==="epic"){
				  body.append('<div class="epic project-chunk">' + item.name + '</div>');
			  }
		  });
	  }
	  
	  http({
		  url: "/api/backlogs/" + backlogId,
          method: "GET",
          onResponse: function (response) {
        	  console.log(response.data);
              var backlog = JSON.parse(response.body);
              render(backlog);
          }
	  });
	  
	  
	  $("#slider").slider();
	  var editButton = $(".edit-button");
	  var commitMessage = $(".commit-message");
	  editButton.button().click(function(){
	      $( ".project-chunk" ).draggable();
	      $(".milestone").draggable();
	      commitMessage.show();
	      editButton.button({ label: "Save" })
	  });
});