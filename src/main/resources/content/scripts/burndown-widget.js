define(["jquery", "d3", "http"], function($, d3, http){

	return function(){

		var chartDiv = d3.select(".chart").append("svg");
		
		
		function draw(data){

			var start = new Date().getTime(), end = new Date().getTime();
			$.each(data, function(idx, item){
				if(item.when < start){
					start = item.when;
				}
				if(item.when > end){
					end = item.when;
				}
			});
			
			console.log("Charting the " + ((end-start)/1000) + " seconds from " + start + " to " + end);
			
			var width = 800;
			var height = 200;
			var xFactor = 1000 ;
			
			function timeScale(t){
				return t/xFactor;
			}
			
			function xValue(item){
				var val = (item.when-start)/xFactor;
				return val;
			}


			var chart = chartDiv
			     .attr("class", "chart")
			     .attr("width", width)
			     .attr("height", height);
			
			

			
			function redraw(){
				
				

				 var dataset =chart.selectAll("rect")
				      .data(data, function(d) {
				    	  var id = d.when;
//				    	  console.log("ID is " + id);
				          return id; 
				      });
//				 
				 
				 dataset.enter().append("line")
					      .attr("x1", xValue)
					      .attr("y1", function(d, i) {
					    	  return height-d.todo;
					       })
					      .attr("x2", function(item, idx){
					    	  if(idx>0){
					    		  var prev = data[idx-1];
					    		  return xValue(prev);
					    	  }else{
					    		  return 0;
					    	  }
					      })
					      .attr("y2", function(item, idx) {
					    	  if(idx>0){
					    		  var prev = data[idx-1];
					    		  return height-prev.todo;
					    	  }else{
					    		  return height;
					    	  }
					       })
					      .style("stroke", "#000")
					      .attr("memo", function(d) { return d.memo; })
					      ;

				 chart.append("line")
				      .attr("x1", 0)
				      .attr("x2", width)
				      .attr("y1", height - .5)
				      .attr("y2", height - .5)
				      .style("stroke", "#000");
				
				
				 dataset.exit().attr("note", function(d){
					 console.log(d.when + " is going to be removed");
				 }).remove();
			}
			
			redraw();
		}
		
		function render(through){
	
			http({
				url:"/api/backlogs/23/statsLog",
				method:"GET",
				onResponse:function(response){
					var data = JSON.parse(response.body).reverse();
					
//					function remove(){
//						console.log("Removing data");
//						data.pop();
//						redraw();
//						setTimeout(remove, 2000);
//					}
//					setTimeout(remove, 2000);
					
					console.log("Threshold is " + through);
					
					if(through){
						var oldLength= data.length;
						data = $.grep(data, function(item){
							return item.when <= through;
						});
						console.log("Data went from " + data.length + " to " +oldLength );
					}
					draw(data);
					
				}
			});
		}
	
		return {render:render};
	}
});