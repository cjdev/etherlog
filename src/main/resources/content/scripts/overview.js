define(["jquery", "underscore"], function($, _){

    return function constructor(body){
        var compareDate = "2014-04-01";
        
        body.append('       <div class="controls-band">' + 
                    '                <div class="controls">' + 
                    '                <h1>Strategic View</h1>' + 
                    '                Compare Date: <input class="compareDateTextField" value="2014-01-01" type="text"></input>' + 
                    '                </div>' + 
                    '        </div>' + 
                    '        <div class="contents">' + 
                    '        </div>');
        
        function MakeProjectBand(project){
            var view = $('<div class="project-band">' + 
                    '    <div class="project-label">' + 
                    '      <a class="title-link" target="backlog" href="/backlog/' + project.id + '">' + project.name + '</a>' + 
                    '      <a class="deltas-link" target="_deltas" href="">deltas</a>' + 
                    '    </div>' + 
                    '    <div class="chart-band">' + 
                    '      <div class="chart-holder"><img class="burndown-chart" src=""></img></div>' + 
                    '    </div>' + 
                    '    <div class="reconciliation-block">' + 
            '    </div>');

            function setDate(date){
                console.log("Setting date to " + date)
                view.find(".deltas-link").attr("href", '/api/backlogs/' + project.id + '/deltas/since-' + date);
                console.log("Setting date to " + view.find(".deltas-link").attr("href"));
                view.find(".burndown-chart").attr("src", '/api/backlogs/' + project.id + '/chart?startDate=2014-01-01&showLatestEvenIfWip=true&showGoalTargetDots=false&showOddWeeks=false&showWeekNumbers=false&endDate=2014-11-01&showMonthLabels&showCompletedWork=false&showGoalLabels=false&compareWith=' + date + '&showMonthVerticals=true&weekStartDay=2014-01-08&showGoalHLines=false&showGoalVLines')
                console.log("Setting date to " + view.find(".deltas-link").attr("href"));
            }
            setDate(compareDate);
            body.find(".contents").append(view);
            return {setDate:setDate};
        }

        var projects = [];

        function renderProjects(){
            body.find(".project-band").remove();
            $.get("/api/backlogs", function(allBacklogs){

                var activeBacklogs = _.filter(allBacklogs, function(b){
                    return b.whenArchived===null;
                });

                function sortByName(a, b){
                    var aName = a.name==null?"unnamed":a.name.toLowerCase();
                    var bName = b.name==null?"unnamed":b.name.toLowerCase(); 
                    return ((aName < bName) ? -1 : ((aName > bName) ? 1 : 0));
                }

                activeBacklogs.sort(sortByName);
                $.each(activeBacklogs, function(idx, project){
                    projects.push(MakeProjectBand(project));
                });
            });
        }

        renderProjects();

        var dateField = body.find('.compareDateTextField');
        dateField.keypress(function(event) {
            if (event.keyCode == 13) {
                compareDate = dateField.val();
                console.log("date is now " + compareDate)
                $.each(projects, function(idx, widget){
                    widget.setDate(compareDate);
                });
            }
        });

        return {};
    };

});