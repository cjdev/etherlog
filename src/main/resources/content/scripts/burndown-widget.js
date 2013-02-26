define(["jquery", "d3", "http"], function($, d3, http){

	return function(backlogId){

		var svg = d3.select(".chart").append("svg");
		
		
		function draw(data){

			var start = new Date().getTime(), end = new Date().getTime();
			var max = 0;
			$.each(data, function(idx, item){
				if(item.todo > max){
					max = item.todo;
				}
				
				if(item.done > max){
					max = item.done;
				}
				
				if(item.when < start){
					start = item.when;
				}
				if(item.when > end){
					end = item.when;
				}
			});
			
			var period = end - start;
//			console.log("Charting the " + (period/1000) + " seconds from " + new Date(start) + " to " + new Date(end));
			
			var width = 800;
			var height = 200;
			var xFactor = 800/period ;
			var yFactor = height/max;
			
			function timeScale(t){
				return t * xFactor;
			}
			
			function xValue(item){
				var val = (item.when-start) * xFactor;
				return val;
			}
			
			function yScale(val){
				return val * yFactor;
			}

			var chart = svg
			     .attr("class", "chart")
			     .attr("width", width)
			     .attr("height", height);
			
			

			
			function redraw(){
				
//				console.log("REDRAWING " + data.length + " items: "+ JSON.stringify(data) );
				
				
				function idFunction(d, i) {
//			    		  console.log(i + " is " + JSON.stringify(d) + "!");
				    	  if(d){
				    		  var id = d.when;
				    		  return id; 
				    	  }
				}
				 var dataset =chart.selectAll("line")
				      .data(data, idFunction);
				 
				 dataset.enter().append("line")
					      .attr("x1", xValue)
					      .attr("y1", function(d, i) {
					    	  return height-yScale(d.todo);
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
					    		  return height-yScale(prev.todo);
					    	  }else{
					    		  return height;
					    	  }
					       })
					      .style("stroke", "blue")
					      .style("stroke-width", "3px")
					      .style("stroke-linecap", "round")
					      .attr("memo", function(d) { return d.memo; })
					      ;


				 
				 chart.append("line")
				      .attr("x1", 0)
				      .attr("x2", width)
				      .attr("y1", height - .5)
				      .attr("y2", height - .5)
				      .style("stroke", "#000");
				
				
				 dataset.exit().remove();
				 

				var circles = chart.selectAll("circle").data(data, idFunction);
			    circles.enter().append("circle")
					      .attr("cx", xValue)
					      .attr("cy", function(d, i) {
					    	  return height-yScale(d.todo);
					       })
					      .attr("r", 3)
					      .style("fill", "blue");
			    circles.exit().remove();
			}
			
			redraw();
		}
		
		function render(through){
	
			http({
				url:"/api/backlogs/" + backlogId + "/statsLog",
				method:"GET",
				onResponse:function(response){
					var data = JSON.parse(response.body).reverse();
					
//					console.log("Threshold is " + through);
					
					if(through){
						var oldLength= data.length;
						data = $.grep(data, function(item){
							return item.when <= through;
						});
//						console.log("Data went from " + data.length + " to " +oldLength );
					}
					draw(data);
					
				}
			});
		}
	
		return {render:render};
	}
});