define(["jquery", "http", "uuid", "underscore"], function($, http, uuid, _){
	 var body = $("body");
	  body.find(".add-backlog-button").click(function(){
		  var name = prompt("Give the backlog a name","some name");
		  if(name===null || !name) return;
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
	  
	  var backlogList = body.find(".backlog-list")
	  
	  http({
		  url: "/api/backlogs",
         method: "GET",
         onResponse: function (response) {
       	 var allBacklogs=JSON.parse(response.body);
       	 var showArchivedCheckbox = body.find(".show-archived-checkbox");
       	 
       	showArchivedCheckbox.click(function(){
       	    var isChecked = showArchivedCheckbox.is(":checked");
       	    var entries = body.find(".archived-backlog-list-entry");
       	    if(isChecked){
       	        entries.slideDown();
       	    }else{
       	        entries.slideUp();
       	    }
       	 });
       	 
       	 var showArchived = body.find(".show-archived-checkbox").is(":checked");
       	 
       	 var backlogs = allBacklogs;//_.filter(allBacklogs, function(b){return showArchived || b.whenArchived===null;});
       	 
       	  function sortByName(a, b){
       	  var aName = a.name==null?"unnamed":a.name.toLowerCase();
       	  var bName = b.name==null?"unnamed":b.name.toLowerCase(); 
       	  return ((aName < bName) ? -1 : ((aName > bName) ? 1 : 0));
       	 }

       	backlogs.sort(sortByName);
       	  
       	  $.each(backlogs, function(idx, backlog){
       	      var entry;
       	      
       	      entry = $('<li class="backlog-list-entry"><a href="/backlog/' + backlog.id + '"></a>  <button class="archive-button">archive</button></li>');
       	      
       	      function isArchived(){
       	          var val = backlog.whenArchived!==null;
       	          console.log('is archived ' + val);
       	          
       	          return val;
       	      }
       	      function redraw(){
       	          var name = isArchived() ? "[ARCHIVED] " + backlog.name : backlog.name;
       	          console.log(name);
       	          entry.find("a").text(name);
       	          if(isArchived()){
           	          entry.addClass("archived-backlog-list-entry")
                      entry.toggle(body.find(".show-archived-checkbox").is(":checked"));
           	      }else {
           	          entry.removeClass("archived-backlog-list-entry")
           	      }
       	          
              }
       	      redraw();
       	      
              entry.find(".archive-button").click(function (){
                   if(!confirm('Are you sure you want to archive "' + backlog.name + '"?')) return;
                   console.log('archive' + backlog.name);
                   
                   http({
                       url: "/api/backlogs/" + backlog.id + "/status",
                       method: "PUT",
                       data:JSON.stringify({
                           archived:!isArchived()
                       }),
                       onResponse: function (response) {
                           if(response.status === 200){
                               console.log(response);
                               backlog = JSON.parse(response.body);
                               redraw();
                           }else{
                               alert("ERROR: " + response.status);
                           }
                       }
                   });
       	      });
       		  body.find(".backlog-list").append(entry);
       	  });
         }
	  });
});