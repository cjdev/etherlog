define([], function(){
	function compareEstimatesByWhen(a, b){
        if(a.when === b.when){
            return 0;
        }else if (a.when > b.when){
            return 1;
        }else{
            return -1;
        }
    }
	function findMostRecentEstimate(item){
	    if(item.estimates && item.estimates.length>0){
	    	var copy = item.estimates.slice();
	    	copy.sort(compareEstimatesByWhen);
	        var mostRecentEstimate = copy[copy.length-1];
	    	for(x=0;x<copy.length;x++){
	    		console.log(x, copy[x]);
	    	}
	        return mostRecentEstimate;
	    }else{
	    	return undefined;
	    }
	}
	
	return {findMostRecentEstimate:findMostRecentEstimate};
	
});

