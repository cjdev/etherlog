define(["jquery"], function($){
    
    return function(container){
        var view = $('<h1>Time Machine</h1>' + 
                       '<h2>The current time is <span class="current-time"></span></h2>' + 
                       '<button class="jump-button">Jump to tomorrow</button>');
        

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
        
        
        var button = $(".jump-button");
        button.click(function(){
            console.log("hi");
            $.ajax("/api/clock", {
                type: 'PUT',
                data: (1000*60*60*24).toString(),
                success: function(data) {
                    updateClockDisplay();
                    alert("Jump Complete.  Welcome to tomorrow!");
                }
              });
        });
    };
});