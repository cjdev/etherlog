define(["jquery", "http", "uuid"], function($, http, uuid){
	  
	
	  var backlog, where, lastDragged;
	  
	  var backlogId = 23; // HACK!
	  
	  var widgets = [];
	  
	  where = $("body");
	  
	  var view = {
	      title : where.find("#title"),
	      backlog : where.find(".backlog"),
	      slider : where.find("#slider"),
	      editButton : where.find(".edit-button"),
	      saveButton : where.find(".save-button"),
	      addStoryButton : where.find(".add-story-button"),
	      addEpicButton : where.find(".add-epic-button"),
	      addGoalButton : where.find(".add-goal-button"),
	      commitMessage : where.find(".commit-message")
	  };
	  
	  
	  
	  function render(){
		  view.title.text(backlog.name);
		  view.backlog.empty();
		  $.each(backlog.items, function(idx, item){
			  DropZone(item.id, view.backlog);
			  widgets.push(ItemWidget(item, view.backlog));
		  });
	  }
	  
	  function showEditMode(){
		  
		  $.each(widgets, function(idx, widget){
			  widget.showEditableMode();
		  });
		  
	      view.editButton.hide();
	      view.commitMessage.show();
	      view.saveButton.show();
	      view.addStoryButton.show();
	      view.addEpicButton.show();
	      view.addGoalButton.show();
	  }
	  
	  function showViewMode(){
	      view.editButton.show();
	      view.commitMessage.hide();
	      view.saveButton.hide();
	      view.addStoryButton.hide();
	      view.addEpicButton.hide();
	      view.addGoalButton.hide();
	      render();
	  }
	  
	  function deleteItem(item){
		  var idx = findItemNumById(item.id);
		  console.log("Deleting #" + idx + " " + JSON.stringify(item));
		  
		  backlog.items.splice(idx, 1);
		  where.find("#" + item.id).remove();
		  where.find("#dropZone" + item.id).remove();
	  }
	  
	  function moveItemBefore(itemId, beforeId){
		  if(itemId!==beforeId){
	        	var subject = where.find("#" + itemId); 
	        	
	        	subject.detach().insertBefore("#dropZone" + beforeId);
	        	subject.css("left", 0);
	        	subject.css("top", 0);
	        	$("#dropZone" + itemId).detach().insertBefore(where.find("#" + itemId));
	        }
	  }
	  
	  function DropZone(id, backlogDiv){
		  var v = $('<div id="dropZone' + id + '" class="drop-zone"></div>');
		  

		  function show(){
			  v.css("visibility", "visible");
		  }
		  function hide(){
			  v.css("visibility", "hidden");
		  }
		  
		  function currentDropIsAcceptable(){
			  return lastDragged.id !==id;
		  }
		  
		  v.droppable({
		      drop: function( event, ui ) {
		        console.log(lastDragged.name + " was dropped on " + id);
		        var subjectId = lastDragged.id;
		        moveItemBefore(subjectId, id);
		        hide();
		      },
		      over: function(event, ui){
		    	  if(currentDropIsAcceptable()){
		    		  show();
		    	  }
		      },
		      out: function(event, ui){
		    	  hide();
		      }
		  });
		  
		  hide();
		  v.appendTo(backlogDiv);
	  }
	  
	  
	  function EstimatesWidget(item, parentDiv){
		  
		  var v = $('<div id="' + item.id + '" class="estimates-list">' + 
					  '<div class="estimate"><select><option></option><option>swag</option><option>grooming</option><option>team</option></select> <input size="2" type="text"></input>   </div>' + 
				  '</div>');
		  var view = {
				  addButton : v.find(".add-button"),
				  currencies : v.find("select"),
				  value : v.find('input')
		  };
		  
		  if(item.estimates && item.estimates.length > 0){
			  const estimate = item.estimates[item.estimates.length-1];
			  view.currencies.val(estimate.currency);
			  view.value.val(estimate.value);
		  }
		  
		  
		  function getEstimateForCurrency(currency){
			  var estimate;
			  if(item.estimates){
				  var matches = $.grep(item.estimates, function(estimate){
					  return estimate.currency === currency;
				  });
				  
				  estimate = matches[0];
			  }else{
				  estimate = undefined;
			  }
			  
			  return estimate;
		  }
		  
		  var oldValue, oldCurrency;
		  
		  function onChange(){
			  var currency, value;
			  
			  currency = view.currencies.val();
			  value = view.value.val();
			  
			  if(value!==oldValue || currency !==oldCurrency){
				  oldValue = value;
				  oldCurrency = currency;
				  console.log("Estimate: " + currency + " " + value);
				  
				  
				  if(currency !== ""){
					  
					 var estimate = getEstimateForCurrency(currency);
					  
					  
					  if(!estimate){
						  console.log("no existing " + currency + " estimate");
						  estimate = {id:uuid()};
						  if(!item.estimates){
							  item.estimates = [];
						  }
						  item.estimates.push(estimate);
					  }else{
						  console.log(estimate);
					  }
					  
					  estimate.currency = currency;
					  estimate.value = value;
					  estimate.when = new Date().getTime();
				  }
				  
			  }
		  }
		  
		  view.currencies.bind("change", onChange);
		  v.bind("keypress keydown keyup change", onChange);
		  
		  v.appendTo(parentDiv);
		  
	  }
	  
	  function ItemWidget(item, backlogDiv){
		  var html, v, view, onDelete;
		  
		  v = $('<div id="' + item.id + '" class="item clearfix">' + 
				  '<img style="display:none;" src="/delete.png"/ class="delete-icon delete-button">' + 
				  '<img style="display:none;" src="/pencil.png"/ class="edit-icon edit-button">' + 
				  '<div class="controls" ><button style="display:none;" class="done-button">Done</button>' + 
				  '<div style="display:none;"class="estimates-holder"></div></div>' + 
				  '<span class="label"/>' + 
				  '<textarea style="display:none;"></textarea></div>');
		  
		  
		  view = {
		      label:v.find(".label"),
		      textarea:v.find("textarea"),
		      editButton:v.find(".edit-button"),
		      deleteButton:v.find(".delete-button"),
		      doneButton:v.find(".done-button"),
		      estimatesHolder: v.find(".estimates-holder")
		  };
		  
		  
		  function compareEstimatesByWhen(a, b){
			  if(a.when === b.when){
				  return 0;
			  }else if (a.when > b.when){
				  return 1;
			  }else{
				  return -1;
			  }
		  }
		  
		  function mostRecentEstimateText(){
			  var result;
			  console.log("Stuff: " + JSON.stringify(item.estimates));
			  
			  if(item.estimates && item.estimates.length>0){
				  item.estimates.sort(compareEstimatesByWhen);
				  const mostRecentEstimate = item.estimates[item.estimates.length-1];
				  result = "(" + mostRecentEstimate.value + " " + mostRecentEstimate.currency + ")";
			  }else{
				  result = "";
			  }
			  
			  return result;
		  }
		  
		  if(item.kind==="goal"){
			  v.addClass("milestone divider clearfix");
			  view.label.text("GOAL: " + item.name);
		  }else {
			  if(item.kind==="story"){
				  v.addClass("story project-chunk");
			  }else if(item.kind==="epic"){
				  v.addClass("epic project-chunk");
			  }
			  view.label.text(item.name + " " + mostRecentEstimateText());
		  }
		  
		  EstimatesWidget(item, view.estimatesHolder);
		  view.textarea.val(item.name);
		  view.textarea.bind("keypress change",function(n){
			  item.name = view.textarea.val();
			  view.label.text(item.name);
			  console.log("Changed to " + item.name);
		  });
		  
		  function makeDraggable(){

			  v.draggable({
				  start: function( event, ui ) {
					  console.log("Started dragging " + item.name);
					  lastDragged = item;
				  	},
				   stop:function(event, ui){
					   console.log("Stopped dragging" + ui.helper.attr('id'));
					   ui.helper.css("left", 0);
					   ui.helper.css("top", 0);
				   }
			  });
		  }
		  
		  function showEditableMode(){
			  view.editButton.show();
			  view.label.show();
			  view.deleteButton.show();
			  
			  view.textarea.hide();
			  view.doneButton.hide();
			  view.estimatesHolder.hide();
			  
			  makeDraggable();
		  }

		  function showEditMode(){
			  view.label.hide();
			  view.editButton.hide();
			  view.deleteButton.hide();
			  

			  view.textarea.show();
			  view.doneButton.show();
			  view.estimatesHolder.show();
			  
			  makeDraggable();
		  }
		  
		  view.doneButton.button().click(showEditableMode);
		  
		  view.editButton.click(showEditMode);
		  view.deleteButton.click(function(){
			  deleteItem(item);
		  });
		  
		  v.appendTo(backlogDiv);
		  
		  
		  function scrollTo(){
			  $('html, body').animate({
		            scrollTop: v.offset().top + 'px'
		        }, 'fast');
		  }
		  
		  
		  return {
			  showEditMode:showEditMode,
			  showEditableMode:showEditableMode,
			  scrollTo:scrollTo
		  };
	  }
	  
	  function findItemNumById(id){
		  var result;
		  
		  $.each(backlog.items, function(idx, item){
			  if(item.id===id){
				  result = idx;
			  }
			  
		  });
		  
		  return result;
	  }
	  
	  function findItemById(id){
		  console.log("Looking for" + id);
		  var matches = $.grep(backlog.items, function(item){
			  console.log(item);
			  const result = item.id===id;
			  console.log(result);
			  return result;
		  });
		  
		  return matches[0];
	  }
	  
	  function readView(){
		  
		  var newList = [];
		  
		  where.find(".project-chunk, .milestone").each(function(idx, domElement){
			  var id = $(domElement).attr("id");
			  var chunk = findItemById(id);
			  
			  console.log("Result: " + JSON.stringify(chunk));
			  console.log("chunk: " + chunk.id + " " + chunk.name + " (" + chunk.kind + ")");
			  
			  newList.push(chunk);
		  });
		  
		  backlog.items = newList;
	  }
	  
	  view.slider.slider();
	  view.editButton.button().click(function(){
		  view.commitMessage.val("");
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
	  
	  
	  function addNewItem(item){

		  console.log(item);
		  DropZone(item.id, view.backlog);
		  const widget = ItemWidget(item, view.backlog);
		  widget.showEditMode();
		  widgets.push(widget);
		  backlog.items.push(item);
		  
		  
		  widget.scrollTo();
	  }
	  
	  view.addStoryButton.button().click(function(){
		  addNewItem({
				  id:uuid(),
				  name:"new story",
				  kind:"story"
		  });
	  });
	  
	  view.addEpicButton.button().click(function(){
		  addNewItem({
				  id:uuid(),
				  name:"new epic",
				  kind:"epic"
		  });
	  });
	  
	  view.addGoalButton.button().click(function(){
		  addNewItem({
				  id:uuid(),
				  name:"new goal",
				  kind:"goal"
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