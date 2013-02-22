define(["jquery", "http"], function($, http){
	  
	
	  var backlog, where;
	  
	  var backlogId = 23; // HACK!
	  
	  where = $("body");
	  
	  var view = {
	      title : where.find("#title"),
	      backlog : where.find(".backlog"),
	      slider : where.find("#slider"),
	      editButton : where.find(".edit-button"),
	      saveButton : where.find(".save-button"),
	      commitMessage : where.find(".commit-message")
	  };
	  
	  
	  
	  function render(){
		  console.log(backlog);
		  view.title.text(backlog.name);
		  view.backlog.empty();
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
	  
	  function showEditMode(){
	      where.find( ".project-chunk" ).draggable();
	      where.find(".milestone").draggable();
	      view.editButton.hide();
	      view.commitMessage.show();
	      view.saveButton.show();
	  }
	  
	  function showViewMode(){
	      view.editButton.show();
	      view.commitMessage.hide();
	      view.saveButton.hide();
	      render();
	  }
	  
	  view.slider.slider();
	  view.editButton.button().click(function(){
		  showEditMode();
	  });
	  
	  view.saveButton.button().click(function(){
		  
		  backlog.memo = view.commitMessage.val();
		  
		  http({
			  url: "/api/backlogs/" + backlogId,
	          method: "PUT",
	          data:JSON.stringify(backlog),
	          onResponse: function (response) {
	        	  showViewMode();
	          }
		  });
		  
	  });
	  

	  http({
		  url: "/api/backlogs/" + backlogId,
          method: "GET",
          onResponse: function (response) {
        	  backlog=JSON.parse(response.body);
              render();
          }
	  });
});