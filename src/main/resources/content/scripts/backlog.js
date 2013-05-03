define(["jquery", "http", "uuid"], function($, http, uuid){

      const kindsInOrderOfPrecedence = ["team", "grooming", "swag"];
    
	  var backlog, where, lastDragged;
	  
	  var when; // SUPERHACK!
	  
	  var backlogId = parseBacklogIdFromQueryString();
	  
	  function parseBacklogIdFromQueryString(){
		  var parts = window.location.toString().split("/");
		  if(parts.length>0){
			  return parts[parts.length-1];
		  }else{
			  return undefined;
		  }
	  }
	  	  
	  var widgets = [];
	  
	  where = $("body");
	  
	  var activityMonitor = (function(){
		  var view = $(".throbber");
		  var items = [];
		  
		  function show(){
			  view.show();
			  var item = {};
			  
			  items.push(item);
			  
			  return {
				  done:function(){
					  items.pop(item);
					  if(items.length===0){
						  view.hide();
					  }
				  }
			  };
		  }
		  function showUnknown(){
			  view.show();
		  }
		  return {
			showUnknown:showUnknown,
			show:show  
		  };
	  }());
	  
	  var view = {
	      title : where.find("#title"),
	      backlog : where.find(".backlog"),
	      slider : where.find("#slider"),
	      summaryTextArea : where.find("#summary"),
	      editButton : where.find(".edit-button"),
	      saveButton : where.find(".save-button"),
	      addStoryButton : where.find(".add-story-button"),
	      addEpicButton : where.find(".add-epic-button"),
	      addGoalButton : where.find(".add-goal-button"),
	      commitMessage : where.find(".commit-message"),
	      memoTextArea : where.find(".memo-text")
	  };
	  
	  var lastServerUpdate = new Date().getTime();
	  var lastChange = lastServerUpdate;
	  
	  function sendUpdate(){
		  const updateInterval = 3000;
		  const t = lastChange;
		  if(t > lastServerUpdate){
			  try{
				  var monitor = activityMonitor.show();
				  readView();
				  backlog.memo = "work-in-progress";
				  
				  var newBacklogText = JSON.stringify(backlog);
				  
				  http({
					  url: "/api/backlogs/" + backlogId ,
					  method: "PUT",
					  data:newBacklogText,
					  onResponse: function (response) {
						  var status = response.status;
//						  console.log("Save queue: Changes submitted with " + status);
						  if(status===200){
							  lastServerUpdate = t;
							  chart.refresh();
							  setTimeout(sendUpdate, 1000);
						  }else{
							  handleUnexpectedError("Response was " + status + ".  I sent:\n" + newBacklogText);
							  setTimeout(sendUpdate, 1000);
						  }
						  monitor.done();
					  }
				  });
			  }catch(e){
				  handleUnexpectedError(e);
				  
				  setTimeout(sendUpdate, 1000);
			  }
		  }else{
			  setTimeout(sendUpdate, 1000);
		  }
	  }
	  
	  sendUpdate();
	  
	  function handleUnexpectedError(e){

		  var error = e?e.toString():"null";
		  
		  http({
			  url: "/api/errors/" ,
			  method: "POST",
			  data:JSON.stringify("Error updating backlog " + backlogId + ":\n" + error),
			  onResponse: function (response) {
			  }
		  });
		  
		  alert("Unexpected error (maybe the server is down or inaccessible?).  The error was:\n " + e);
	  }
	  
	  function sendWorkInProgress(){
		  activityMonitor.showUnknown();
		  lastChange = new Date().getTime();
	  }
	  
	  
	  function calculateTotals(backlog){
	      var totals = {};
          
          $.each(kindsInOrderOfPrecedence, function(idx, kind){
              totals[kind] = 0;
          });
          
          function findBestEstimate(backlogItem){
              var bestEstimate;
              var estimates = backlogItem.estimates;
              if(estimates){
                  $.each(kindsInOrderOfPrecedence, function(idx, kind){
                      
                          $.each(estimates, function(idx, estimate){
                              if(!bestEstimate){
                                  bestEstimate = {type:estimate.currency, value:estimate.value};
                              } 
                          });
                  });
              }
              return bestEstimate;
          }
          
          $.each(backlog.items, function(idx, item){
              var bestEstimate = findBestEstimate(item);
              if(bestEstimate){
                  totals[bestEstimate.type] += bestEstimate.value;
              }
          });
          return totals;
	  }
	  
	  function formatLongDateTime(millis){
	      var d = new Date(millis);
	      
	      function pad(v){
	          return v<10?"0" + v:v;
	      }
	      
	      return d.getFullYear() + "-" + pad(d.getMonth()) + "-" +  pad(d.getDate());
	  }
	  
	  function render(){
	      
		  view.memoTextArea.text(formatLongDateTime(when?when:new Date().getTime()) + ": " + backlog.memo);
		  view.title.text(backlog.name);
		  view.backlog.empty();
		  
		  $.each(backlog.items, function(idx, item){
			  DropZone(item.id, view.backlog);
			  widgets.push(ItemWidget(item, view.backlog));
		  });

		  var totals = calculateTotals(backlog); 
          view.summaryTextArea.text("(" + $.map(totals, function(value, key){return key + " " + value + "  ";}) + ")");
          
		  
		  chart.render(when);
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
		  view.memoTextArea.css("visibility", "visible");
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
		  backlog.items.splice(idx, 1);
		  where.find("#" + item.id).remove();
		  where.find("#dropZone" + item.id).remove();
		  sendWorkInProgress();
	  }
	  
	  function moveItemBefore(itemId, beforeId){
		if(itemId!==beforeId){
		    var subject = where.find("#" + itemId); 
			
			subject.detach().insertBefore("#dropZone" + beforeId);
			subject.css("left", 0);
			subject.css("top", 0);
			$("#dropZone" + itemId).detach().insertBefore(where.find("#" + itemId));
		}
		sendWorkInProgress();
	  }
	  
	  function HistorySlider(sliderDiv){
		  var history;
		  
		  function showCurrent(){
			  sliderDiv.slider("value", history.length-1);
		  }
		  
		  function refresh(){
			  http({
		    	  url:'/api/backlogs/' + backlogId + '/history',
		    	  method:"GET",
		    	  onResponse:function(response){
		    		  history = JSON.parse(response.body).reverse();
//		    		  console.log("There are " + history.length + " items in the history");
		    		  
		    		  sliderDiv.slider({
					      value:history.length-1,
					      min: 0,
					      max: history.length-1,
					      step: 1,
					      slide: function( event, ui ) {
					    	  var selection = ui.value;
					    	  var i = history[selection];
					    	  console.log(ui.value + " " + i.version + "(" + i.memo + ") on " + i.when);
					    	  when = i.when;
					    	  showVersion(i.version);
					      }
					    });
		    		  
		    	  }
		      });
		  }
		  
		  refresh();
	      
	      return {
	    	  showCurrent:showCurrent,
	    	  refresh:refresh
	      };
	  }
	  
	  function showVersion(version){
		  var monitor = activityMonitor.show();
		  
		  http({
			  url:"/api/backlogs/" + backlogId + "/history/" + version,
			  method:"GET",
			  onResponse:function(response){
				  view.memoTextArea.css("visibility", "visible");
				  backlog = JSON.parse(response.body);
				  render();
				  monitor.done();
			  }
		  });
		  

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
				  

				 if(value<=0 && oldValue){
					 alert("You must specify a value");
					 view.value.val(oldValue);
				 }else{
					 oldValue = value;
					 oldCurrency = currency;
//					 console.log("Estimate: " + currency + " " + value);
					 
					 
					 if(currency !== ""){
						 var estimate = getEstimateForCurrency(currency);
						 
						 
						 if(!estimate){
//							 console.log("no existing " + currency + " estimate");
							 estimate = {id:uuid()};
							 if(!item.estimates){
								 item.estimates = [];
							 }
							 item.estimates.push(estimate);
						 }else{
//							 console.log(estimate);
						 }
						 
						 estimate.currency = currency;
						 estimate.value = value;
						 estimate.when = new Date().getTime();
						 
						 sendWorkInProgress();
					 }
				 }
				  
			  }
		  }
		  
		  view.currencies.bind("change", onChange);
		  v.bind("keypress keydown keyup change", onChange);
		  
		  v.appendTo(parentDiv);
		  
	  }
	  
	  
	  function ItemWidget(item, backlogDiv){
		  var html, v, view, onDelete, showViewMode;
		  const finishedCssClass = "finished";
		  
		  v = $('<div id="' + item.id + '" class="item clearfix">' + 
				  '<img style="display:none;" src="/delete.png"/ class="delete-icon delete-button">' + 
				  '<img style="display:none;" src="/pencil.png"/ class="edit-icon edit-button">' + 
				  '<img style="display:none;" src="/medal.png"/ class="finished-icon finished-button">' + 
				  '<div class="controls" ><button style="display:none;" class="done-button">Done</button>' + 
				  '<div style="display:none;"class="estimates-holder"></div></div>' + 
				  '<span class="label"/>' + '<div class="remainder" style="display:none;"/>' +
				  '<textarea style="display:none;"></textarea></div>');
		  
		  
		  view = {
		      label:v.find(".label"),
		      remainder:v.find(".remainder"),
		      textarea:v.find("textarea"),
		      finishedButton:v.find(".finished-button"),
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
			  
			  if(item.estimates && item.estimates.length>0){
				  item.estimates.sort(compareEstimatesByWhen);
				  const mostRecentEstimate = item.estimates[item.estimates.length-1];
				  result = "(" + mostRecentEstimate.value + " " + mostRecentEstimate.currency + ")";
			  }else{
				  result = "";
			  }
			  
			  return result;
		  }
		  
		  var showViewMode;
		  
		  
		  
		  function setText(text, decoration){
			  var lines = text.split('\n');
			  if(lines.length>0){
				  var firstLine = lines[0];
				  
				  var label = firstLine;
				  if(decoration){
					  label = label + " " + decoration; 
				  }
				  
				  view.label.text(label);
				  
				  if(lines.length>1){
					  var remainder = text.substring(firstLine.length);
					  view.remainder.text(remainder);
				  }
			  }
		  }
		  
		  view.label.click(function(){
			  if(view.remainder.css("display")==="none"){
				  view.remainder.slideDown();
			  }else{
				  view.remainder.slideUp();
			  }
		  });
		  
		  if(item.kind==="goal"){
			  v.addClass("milestone divider clearfix");
			  showViewMode = function(){
				  setText("GOAL: " + item.name);
			  }
		  }else {
			  if(item.kind==="story"){
				  v.addClass("story project-chunk");
				  if(item.isComplete){
					  v.addClass(finishedCssClass);
				  }
			  }else if(item.kind==="epic"){
				  v.addClass("epic project-chunk");
			  }
			  showViewMode = function(){
				  setText(item.name, mostRecentEstimateText());
			  }
			  EstimatesWidget(item, view.estimatesHolder);
		  }

		  
		  
		  showViewMode();
		  
		  view.textarea.val(item.name);
		  view.textarea.bind("keypress change",function(n){
			  item.name = view.textarea.val();
			  view.label.text(item.name);
			  sendWorkInProgress();
		  });
		  
		  function makeDraggable(){

			  v.draggable({
				  start: function( event, ui ) {
					  lastDragged = item;
				  	},
				   stop:function(event, ui){
					   ui.helper.css("left", 0);
					   ui.helper.css("top", 0);
				   }
			  });
		  }
		  
		  function showEditableMode(){
			  showViewMode();
			  view.editButton.show();
			  view.label.show();
			  view.remainder.hide();
			  if(!item.isComplete) {
				  view.deleteButton.show();
			  }
			  if(item.kind==="story"){
				  view.finishedButton.show();
			  }
			  
			  view.textarea.hide();
			  view.doneButton.hide();
			  view.estimatesHolder.hide();
			  
			  makeDraggable();
		  }

		  function showEditMode(){
			  view.label.hide();
			  view.remainder.hide();
			  view.editButton.hide();
			  view.deleteButton.hide();
			  view.finishedButton.hide();
			  

			  view.textarea.show();
			  view.textarea.focus();
			  view.doneButton.show();
			  view.estimatesHolder.show();
			  
			  makeDraggable();
		  }
		  
		  view.doneButton.button().click(showEditableMode);
		  
		  view.finishedButton.click(function(){
			  if(item.isComplete){
				  item.isComplete = false;
				  v.removeClass(finishedCssClass);
			  }else{
				  item.isComplete = true;
				  v.addClass(finishedCssClass);
			  }
			  sendWorkInProgress();
		  });
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
		  var matches = $.grep(backlog.items, function(item){
			  const result = item.id===id;
			  return result;
		  });
		  
		  return matches[0];
	  }
	  
	  function readView(){
		  
		  var newList = [];
		  
		  where.find(".project-chunk, .milestone").each(function(idx, domElement){
			  var id = $(domElement).attr("id");
			  var chunk = findItemById(id);
			  
			  newList.push(chunk);
		  });
		  backlog.items = newList;
		  backlog.memo = view.commitMessage.val();
	  }

	  var slider = HistorySlider(view.slider);
	  
	  view.editButton.button().click(function(){
		  showCurrentVersion(function(){
			  view.commitMessage.val("");
			  showEditMode();
			  slider.showCurrent();
			  view.memoTextArea.css("visibility", "hidden");
		  });
	  });
	  
	  view.saveButton.button().click(function(){
		  
		  readView();
    	  
		  http({
			  url: "/api/backlogs/" + backlogId,
	          method: "PUT",
	          data:JSON.stringify(backlog),
	          onResponse: function (response) {
	        	  showViewMode();
	        	  slider.refresh();
	          }
		  });
		  
	  });
	  
	  
	  function addNewItem(item){

		  DropZone(item.id, view.backlog);
		  const widget = ItemWidget(item, view.backlog);
		  widget.showEditMode();
		  widgets.push(widget);
		  backlog.items.push(item);
		  
		  widget.scrollTo();
		  
		  sendWorkInProgress();
	  }
	  
	  view.addStoryButton.button().click(function(){
		  addNewItem({
				  id:uuid(),
				  name:"",
				  kind:"story"
		  });
	  });
	  
	  view.addEpicButton.button().click(function(){
		  addNewItem({
				  id:uuid(),
				  name:"",
				  kind:"epic"
		  });
	  });
	  
	  view.addGoalButton.button().click(function(){
		  addNewItem({
				  id:uuid(),
				  name:"",
				  kind:"goal"
		  });
	  });
	  
	  function showCurrentVersion(fn){
		  var monitor = activityMonitor.show();
		  http({
			  url: "/api/backlogs/" + backlogId,
	          method: "GET",
	          onResponse: function (response) {
	        	  backlog=JSON.parse(response.body);
	              render();
	              if(fn){
	            	  fn();
	              }
				  monitor.done();
	          }
		  });
	  }

	  var chart = (function(){
		  
		  function render(when){
			  var monitor = activityMonitor.show();
			  var url = "/api/backlogs/" + backlogId + "/chart";
			  
			  if(when){
				  url = url+"?end=" + when + "&showLatestEvenIfWip=true";
			  }
			  
			  $("img.chart").attr("src", url);
			  monitor.done();
		  }
		  
		  function refresh(){
			  render(new Date().getTime())
		  }
		  
		  return {
			  render:render,
			  refresh:refresh
	  }}());
	  
	  showCurrentVersion();
	  
	  
});