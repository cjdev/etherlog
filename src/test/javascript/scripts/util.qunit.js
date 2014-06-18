define(["underscore", "util"], function(_, Util){

	test("finds most recent estimate", function(){
		//given
		var item = {"id":"6DDB2CF6-728B-47B3-8072-02FB692908CB",
				    "isComplete":false,
				    "name":"fdsfds",
				    "kind":"story",
				    "estimates":[{"id":"A5762152-4CDB-497D-8FE8-997AB5BD21B7","currency":"swag","value":33,"when":1403099924467},
				                 {"id":"E80083D2-FF2B-4604-817D-E0DF2A2A59D3","currency":"grooming","value":33,"when":1403099919184},
				                 {"id":"745B6C30-80FC-4433-A28D-D20FAC4195E0","currency":"team","value":33,"when":1403099363315}],
				    "when":null};
		
		// when
		var result = Util.findMostRecentEstimate(item);
		
		// then
		deepEqual(result, {"id":"A5762152-4CDB-497D-8FE8-997AB5BD21B7","currency":"swag","value":33,"when":1403099924467})
	});
	
});