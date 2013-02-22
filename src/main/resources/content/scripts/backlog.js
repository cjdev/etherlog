define(["jquery", "http"], function($, http){
	  
	
	  var backlog, where;
	  
	  var backlogId = 23; // HACK!
	  
	  var widgets = [];
	  
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
			  widgets.push(ItemWidget(item, view.backlog));
		  });
	  }
	  
	  function showEditMode(){
		  
		  $.each(widgets, function(idx, widget){
			  widget.showEditMode();
		  });
		  
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
	  
	  
	  function ItemWidget(item, backlogDiv){
		  var html, v, view;

		  v = $('<div id="' + item.id + '" class="item"><span class="label"/><textarea style="display:none;"></textarea><img style="display:none;" src="/pencil.png"/ class="edit-icon edit-button"><button style="display:none;" class="done-button">Done</button></div>');
		  
		  view = {
		      label:v.find(".label"),
		      textarea:v.find("textarea"),
		      editButton:v.find(".edit-button"),
		      doneButton:v.find(".done-button")
		  };
		  
		  if(item.kind==="goal"){
			  v.addClass("milestone divider clearfix");
			  view.label.text("GOAL: " + item.name);
		  }else {
			  if(item.kind==="story"){
				  v.addClass("story project-chunk");
			  }else if(item.kind==="epic"){
				  v.addClass("epic project-chunk");
			  }
			  view.label.text(item.name);
		  }
		  
		  view.textarea.val(item.name);
		  view.textarea.bind("keypress change",function(n){
			  item.name = view.textarea.val();
			  view.label.text(item.name);
			  console.log("Changed to " + item.name);
		  });
		  
		  function showEditableMode(){
			  view.editButton.show();
			  view.label.show();
			  view.textarea.hide();
			  view.doneButton.hide();
		  }

		  function showEditMode(){
			  view.label.hide();
			  view.textarea.show();
			  view.editButton.hide();
			  view.doneButton.show();
		  }
		  
		  view.doneButton.button().click(showEditableMode);
		  
		  view.editButton.click(showEditMode);
		  
		  v.appendTo(backlogDiv);
		  
		  return {
			  showEditMode:showEditableMode
		  };
	  }
	  
	  function readView(){
		  
		  var newList = [];
		  
		  where.find(".project-chunk, .milestone").each(function(idx, domElement){
			  var id = $(domElement).attr("id");
			  console.log("Looking for" + id);
			  var matches = $.grep(backlog.items, function(item){
				  console.log(item);
				  const result = item.id===id;
				  console.log(result);
				  return result;
			  });
			  
			  var chunk = matches[0];

			  console.log("Result: " + JSON.stringify(chunk));
			  console.log("chunk: " + chunk.id + " " + chunk.name + " (" + chunk.kind + ")");
			  
			  newList.push(chunk);
		  });
		  
		  backlog.items = newList;
	  }
	  
	  view.slider.slider();
	  view.editButton.button().click(function(){
		  showEditMode();
	  });
	  
	  view.saveButton.button().click(function(){
		  
		  backlog.memo = view.commitMessage.val();
		  readView();
		  
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