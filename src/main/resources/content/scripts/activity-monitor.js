define(["jquery"],
    function($) {
        return function(view, onActivityStart, onActivityStop) {
            var items = [];

            function show(){
                onActivityStart();
                var item = {};

                items.push(item);

                return {
                    done:function(){
                        items.pop();
                        if(items.length===0){
                            onActivityStop();
                        }
                    }
                };
            }
            function showUnknown(){
                onActivityStart();
            }
            return {
                showUnknown:showUnknown,
                show:show
            };
        }
});
