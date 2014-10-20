define(["jquery", "jqueryui", "http"],
    function ($, jqueryui, http) {

        return function(sliderDiv, backlogId, onChange){
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

                        sliderDiv.slider({
                            value:history.length-1,
                            min: 0,
                            max: history.length-1,
                            step: 1,
                            slide: function( event, ui ) {
                                var selection = ui.value;
                                var i = history[selection];
                                onChange(i.version, i.when);
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
    }
);
