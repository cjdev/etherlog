define(["jquery", "underscore", "http"], function($, _, http){
    var teamName = window.location.pathname.replace("/team/", "");
    console.log("Team name is ", teamName);
    
    var teams = [
                 {id:"34243243jfjdska", name:"NaN", 
                     wikiUrl:"http://cjpad.cj.com/NaN-Iteration-",
                     iterations:[
                                 {start:new Date(),

                                     summary:'2 Added Stories/Epics (35 points)\n' + 
                                     '1 Reopened stories (8 points)\n' + 
                                     '1 re-estimated stories/epics (48 points)\n' + 
                                     '2 Completed stories (4 points)\n',
                                     projects:[
                                               {name:"Mobile Download V2",
                                                description:'1 Added Stories/Epics (33 points)\n' + 
                                                            '1 Reopened stories (8 points)\n' + 
                                                            '1 re-estimated stories/epics (48 points)'
                                                },
                                                {name:"NaN Bugs",
                                                    description:'1 Added Stories/Epics (2 points)\n' + 
                                                                '1 Completed story (2 points)\n'
                                                },
                                                {name:"NaN BI Requests",
                                                    description:'1 Completed story (2 points)\n'
                                                }
                                              ]
                                 },
                                 {start:new Date(2014, 4, 5)}
                                 ]}
                 ];
    
    function formatTimestamp(d){
        
        function padZero(i){
            var s = i.toString();
            if(s.length==1) return "0" + s;
            else return s;
        }
        
        return d.getFullYear() + '-' + padZero(d.getMonth() +1 ) + '-' + padZero(d.getDate());
    }
    http({
        url:"/api/team",
        method:"GET",
        onResponse:function(result){
            teams = JSON.parse(result.body);
            
            var team = _.find(teams, function(t){ return t.name === teamName;});
            console.log("Team is ", team);
            view.find(".team-name").text("Team: " + teamName);
//            _.each(teams, function(team){
                var addIterationButton = view.find(".new-iteration-button").click(function(){
                    var result = confirm("Start a new iteration right now?");
                    if(result){
                        http({
                            url:"/api/team/" + team.id + "/iteration",
                            method:"POST",
                            data:JSON.stringify({}),
                            onResponse:function(response){
                                if(response.status===200){
                                    window.location.reload();
                                }
                            }
                        });
                    }
                });

                _.each(team.iterations, function(iteration){
                    var timestampString = iteration.label;//formatTimestamp();
                    var iterationDiv = $('<div style="font-size:12pt;"> <a href="/team/' + team.name + '/iterations/' + timestampString + '">' + timestampString + '</a> [<a href="' + (team.wikiUrl + timestampString) + '">wiki</a>]' + 
//                            '<pre>TOTALS\n' + iteration.summary + '</pre>' + 
                    '</div>');
                    view.append(iterationDiv);
//                    _.each(iteration.projects, function(project){
//                        var projectDiv = $('<div>' + project.name + '<pre style="margin-left:20px;">' + project.description + '</pre></div>');
//                        iterationDiv.append(projectDiv);
//                    });
                });
//            });
        }
    });
    
    
    
    
    
});