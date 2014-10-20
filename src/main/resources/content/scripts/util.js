define(["jquery"], function($){
	function compareEstimatesByWhen(a, b){
        if(a.when === b.when){
            return 0;
        }else if (a.when > b.when){
            return 1;
        }else{
            return -1;
        }
    }

    function formatLongDateTime(millis) {
        var d = new Date(millis);
        function pad(v) {
            return v < 10 ? "0" + v : v;
        }
        return d.getFullYear() + "-" + pad(d.getMonth()+1) + "-" + pad(d.getDate());
    }

    function findMostRecentEstimate(item){
	    if(item.estimates && item.estimates.length>0){
	    	var copy = item.estimates.slice();
	    	copy.sort(compareEstimatesByWhen);
	        var mostRecentEstimate = copy[copy.length-1];
	    	for(x=0;x<copy.length;x++){
	    		//console.log(x, copy[x]);
	    	}
	        return mostRecentEstimate;
	    }else{
	    	return undefined;
	    }
	}

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

	return {
        findMostRecentEstimate:findMostRecentEstimate,
        formatLongDateTime: formatLongDateTime,
        getTime: getTime
    };

});

