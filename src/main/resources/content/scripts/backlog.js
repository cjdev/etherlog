define(["jquery", "http"], function($, http){
	  
	
	  var backlogId = 23; // HACK!
	  
	  var where = $("body");
	  
	  var view = {
	      title : where.find("#title"),
	      backlog : where.find(".backlog"),
	      slider : where.find("#slider"),
	      editButton : where.find(".edit-button"),
	      commitMessage : where.find(".commit-message")
	  };
	  
	  
	  
	  function render(backlog){
		  console.log(backlog);
		  view.title.text(backlog.name);
		  
		  $.each(backlog.items, function(idx, item){
			  var html;
			  if(item.kind==="goal"){
				  html = '<div class="milestone divider clearfix">GOAL: ' + item.name + '</div>';
			  }else if(item.kind==="story"){
				  html = '<div class="story project-chunk">' + item.name + '</div>';
			  }else if(item.kind==="epic"){
				  html = '<div class="epic project-chunk">' + item.name + '</div>';
			  }
			  view.backlog.append(html);
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
	  
	  
	  view.slider.slider();
	  view.editButton.button().click(function(){
	      where.find( ".project-chunk" ).draggable();
	      where.find(".milestone").draggable();
	      view.commitMessage.show();
	      view.editButton.button({ label: "Save" })
	  });
});