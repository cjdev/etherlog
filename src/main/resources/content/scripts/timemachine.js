define(["jquery"], function($){
    
    return function(container){
        var view = $('<h1>Time Machine</h1>' + 
                       '<h2>The current time is <span class="current-time"></span></h2>' +
                       '<div class="controls">' + 
                       '<button class="jump-day">Jump 1 Day</button>' + 
                       '<button class="jump-week">Jump 1 Week</button>' + 
                       '<button class="jump-month">Jump 1 Month</button>' + 
                       '</div>');
        

        function getTime(){
            var millis;
            $.ajax("/api/clock", {
                async:false,
                success:function(r){
                    millis = parseInt(r,10);
                }
            });
            
            return millis;
        }
        
        function updateClockDisplay(){
            var now = getTime();
            view.find(".current-time").text(new Date(now));
            setTimeout(updateClockDisplay, 1000);
        }
        
        updateClockDisplay();
        
        
        
        view.appendTo(container);
        
        function jump(jumpSizeInMillis){
            var controls = view.find(".controls");
            controls.attr("disabled", "disabled");
            $.ajax("/api/clock", {
                type: 'PUT',
                data: jumpSizeInMillis.toString(),
                success: function(data) {
                    updateClockDisplay();
//                    alert("Jump Complete.  Welcome to tomorrow!");
                    
                    controls.removeAttr("disabled");
                }
              });
        }
        const oneDay = 1000*60*60*24; 
        view.find("button.jump-day").click(function(){
            jump(oneDay);
        });
        view.find("button.jump-week").click(function(){
            jump(oneDay * 7);
        });
        view.find("button.jump-month").click(function(){
            jump(oneDay * 30);
        });
        
    };
});