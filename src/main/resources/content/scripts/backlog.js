define(["jquery", "jqueryui", "underscore", "http", "uuid", "util"], function($, jqueryui, _, http, uuid, Util){
    const kindsInOrderOfPrecedence = ["team", "grooming", "swag"];

    var globalConfig;
    http({
        url: "/api/config",
        method: "GET",
        onResponse: function (response) {
            globalConfig = JSON.parse(response.body);
        }
    });

    var backlog, where, lastDragged;
    var when; // SUPERHACK!
    var latestVersion;
    var backlogId = parseBacklogIdFromURL();

    function parseBacklogIdFromURL(){
        var parts = window.location.toString().split("#")[0].split("/");
        if(parts.length>0){
            return parts[parts.length-1];
        }else{
            return undefined;
        }
    }

    var widgets = [];

    where = $("body");

    var activityMonitor = (function(){
        var view = $(".throbber");
        var items = [];

        function show(){
            view.show();
            var item = {};

            items.push(item);

            return {
                done:function(){
                    items.pop(item);
                    if(items.length===0){
                        view.hide();
                    }
                }
            };
        }
        function showUnknown(){
            view.show();
        }
        return {
            showUnknown:showUnknown,
            show:show
        };
    }());

    var view = {
            titleSpan: where.find("#title-span"),
            title : where.find("#title"),
            renameButton: where.find(".rename-icon"),
            backlog : where.find(".backlog"),
            slider : where.find("#slider"),
            summaryTextArea : where.find("#summary"),
            toggleBurndownButton: where.find(".toggle-burndown-button"),
            hideButton : where.find(".hide-button"),
            statsButton : where.find(".stats-button"),
            editButton : where.find(".edit-button"),
            saveButton : where.find(".save-button"),
            addStoryButton : where.find(".add-story-button"),
            addEpicButton : where.find(".add-epic-button"),
            addGoalButton : where.find(".add-goal-button"),
            commitMessage : where.find(".commit-message"),
            memoTextArea : where.find(".memo-text"),
            velocityTextField : where.find(".velocity-text"),
            velocityDiv : where.find(".velocity-div")
    };


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

    var lastServerUpdate = getTime();
    var lastChange = lastServerUpdate;

    function sendUpdate(){
        const updateInterval = 3000;
        const t = lastChange;
        if(t > lastServerUpdate){
            try{
                var monitor = activityMonitor.show();
                readView();
                backlog.memo = "work-in-progress";

                var newBacklogText = JSON.stringify(backlog);

                http({
                    url: "/api/backlogs/" + backlogId ,
                    method: "PUT",
                    data:newBacklogText,
                    onResponse: function (response) {
                        var status = response.status;
                        if(status===200){
                            lastServerUpdate = t;
                            chart.refresh();
                            backlog.optimisticLockVersion = JSON.parse(response.body).optimisticLockVersion;
                            updateSummary();
                            setTimeout(sendUpdate, 1000);
                        }else if(status===409){
                            alert("Looks like someone else is editing this behind our back!  We need to reload.");
                            window.location.reload();
                        }else{
                            handleUnexpectedError("Response was " + status + ".  I sent:\n" + newBacklogText);
                            setTimeout(sendUpdate, 1000);
                        }
                        monitor.done();
                    }
                });
            }catch(e){
                handleUnexpectedError(e);

                setTimeout(sendUpdate, 1000);
            }
        }else{
            setTimeout(sendUpdate, 1000);
        }
    }
    sendUpdate();

    function handleUnexpectedError(e){
        var error = e?e.toString():"null";

        http({
            url: "/api/errors/" ,
            method: "POST",
            data:JSON.stringify("Error updating backlog " + backlogId + ":\n" + error),
            onResponse: function (response) {
            }
        });

        alert("Unexpected error (maybe the server is down or inaccessible?).  The error was:\n " + e);
    }

    function sendWorkInProgress(){
        activityMonitor.showUnknown();
        lastChange = getTime();
    }

    function renderChanges(changes){
        function summarize(change, label){
            if(change.items.length>0){
                return "             " +
                        change.items.length + " " + label + " (" + change.totalPoints + " points)\n";
            }else{
                return "";
            }
        }
        var text=   summarize(changes.added, "Added Stories/Epics") +
                    summarize(changes.removed,"Removed Stories/Epics") +
                    summarize(changes.finished, "Completed stories") +
                    summarize(changes.reopened, "Reopened stories") +
                    summarize(changes.reestimated, "re-estimated stories/epics");

        return text;

    }

    function CommitDialog(){
        var view = $(".commit-dialog"), publishButton, cancelButton;

        publishButton = view.find(".publish-button").button();
        cancelButton = view.find(".cancel-button").button();

        function toggleStuff(){
            $.each([".floatingHeader",  ".save-button", ".hide-button", ".velocity-div",
                    ".add-story-button", ".add-epic-button", ".add-goal-button"], function(idx, i){
                $(i).toggle();
            });
        }

        function closeDialog(){
            $(".commit-dialog").fadeOut(function(){
                $(".backlog").fadeIn();
                toggleStuff();
            });
        }

        cancelButton.click(function(){
            closeDialog();
        });

        publishButton.click(function(){
          var memo = view.find(".commit-message").val();
          // TODO: Add some basic memo validation here, 'cause empty memos are a pain
          backlog.memo = memo;
          http({
              url: "/api/backlogs/" + backlogId,
              method: "PUT",
              data:JSON.stringify(backlog),
              onResponse: function (response) {
                  closeDialog();
                  showViewMode();
                  slider.refresh();
                  window.location.reload(); // HACK!  TODO: remove this when you figure out the "chart doesn't refresh after saving" bug
              }
          });
        });

        function show(){
            http({
                url: "/api/backlogs/" + backlogId + "/deltas/since-last-published",
                method: "GET",
                onResponse: function (response) {
                    var changes = JSON.parse(response.body);

                    view.find(".summaries").text(renderChanges(changes));
                }
            });
            toggleStuff();
            $(".chart").slideDown();
            $(".backlog").fadeOut(function(){
                $(".commit-dialog").fadeIn();
                $(".commit-message").focus();
            });
        }

        return {
            show:show
        };
    }

    var commitDialog = CommitDialog();

    function calculateTotals(items){
        var totals = {};

        $.each(kindsInOrderOfPrecedence, function(idx, kind){
            totals[kind] = 0;
        });

        $.each(items, function(idx, item){
            var bestEstimate = Util.findMostRecentEstimate(item);
            if(bestEstimate){
                totals[bestEstimate.currency] = totals[bestEstimate.currency] + parseInt(bestEstimate.value, 10);
            }
        });
        return totals;
    }

    function formatLongDateTime(millis){
        var d = new Date(millis);

        function pad(v){
            return v<10?"0" + v:v;
        }

        return d.getFullYear() + "-" + pad(d.getMonth()+1) + "-" +  pad(d.getDate());
    }

    function render(){

        view.velocityTextField.val(backlog.projectedVelocity);
        view.memoTextArea.text("Last change " + formatLongDateTime(when?when:getTime()) + ": " + backlog.memo);
        view.title.text(backlog.name);
        $("title").text(backlog.name); // << HACK!
        view.backlog.empty();

        $.each(backlog.items, function(idx, item){
            DropZone(item.id, view.backlog);
            widgets.push(ItemWidget(item, view.backlog));
        });
        updateSummary();
        chart.render(when);
    }

    function updateSummary(){
        var itemsNotDone = _.filter(backlog.items, function(item){return !item.isComplete;});
        var itemsDone = _.filter(backlog.items, function(item){return item.isComplete;});

        function printStuff(stuff){
            return $.map(stuff, function(value, key){return key + " " + value + "  ";})
        }
        var totalsTodo = calculateTotals(itemsNotDone);

        var changesSinceLastPublishedVersion = fetchChanges("in-" + backlog.optimisticLockVersion);

        var summaryText;
        if(changesSinceLastPublishedVersion){
            var averageVelocityText = "";
            var weekSpans = [2, 6, 12];
            _.each(weekSpans, function(numWeeks){
                var nWeeksAgo = formatLongDateTime(when - (1000 * 60 * 60* 24 * 7 * numWeeks));
                var changesOverPastNWeeks = fetchChanges("from-" + nWeeksAgo + "-to-" + formatLongDateTime(when));
                var averageVelocity;
                if(changesOverPastNWeeks){
                    averageVelocity = (changesOverPastNWeeks.finished.totalPoints / numWeeks).toFixed(2);
                }else{
                    averageVelocity = "[not enough info]";
                }
                averageVelocityText += "\n" + numWeeks + " week average weekly velocity: " + averageVelocity;
            });


            summaryText = renderChanges(changesSinceLastPublishedVersion) + "\n" +
                    "TODO: " + printStuff(totalsTodo) + "\n" +
                    'DONE: ' + printStuff(calculateTotals(itemsDone)) + "\n" +
                    averageVelocityText;
        }else{
            summaryText = "";
        }

        view.summaryTextArea.text(summaryText);
    }

    function fetchChanges(expression){
        var changes;
        http({
            url:"/api/backlogs/" + backlog.id + "/deltas/" + expression,
            method: "GET",
            onResponse: function (response) {
                if(response.status==200){
                    changes = JSON.parse(response.body);
                }

            }
        }, {async:false});
        return changes;
    }

    function showEditMode(){

        $.each(widgets, function(idx, widget){
            widget.showEditableMode();
        });

        view.renameButton.data("state", "on");
        view.editButton.hide();
        view.commitMessage.show();
        view.saveButton.show();
        view.addStoryButton.show();
        view.addEpicButton.show();
        view.addGoalButton.show();
        view.velocityDiv.slideDown();
        view.slider.slideUp();
    }

    function showViewMode(){
        view.renameButton.data("state", "off");
        view.memoTextArea.css("visibility", "visible");
        view.editButton.show();
        view.commitMessage.hide();
        view.saveButton.hide();
        view.addStoryButton.hide();
        view.addEpicButton.hide();
        view.addGoalButton.hide();
        view.velocityDiv.hide();
        view.slider.show();
        render();
    }

    function deleteItem(item){
        var idx = findItemNumById(item.id);
        backlog.items.splice(idx, 1);
        where.find("#" + item.id).remove();
        where.find("#dropZone" + item.id).remove();
        sendWorkInProgress();
    }

    function moveItemBefore(itemId, beforeId){
        if(itemId!==beforeId){
            var subject = where.find("#" + itemId);

            subject.detach().insertBefore("#dropZone" + beforeId);
            subject.css("left", 0);
            subject.css("top", 0);
            $("#dropZone" + itemId).detach().insertBefore(where.find("#" + itemId));
        }
        sendWorkInProgress();
    }

    function HistorySlider(sliderDiv){
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
                            showVersion(i.version, i.when);
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

    function showVersion(version, vWhen, fn){
        var monitor = activityMonitor.show();

        http({
            url:"/api/backlogs/" + backlogId + "/history/" + version,
            method:"GET",
            onResponse:function(response){
                view.memoTextArea.css("visibility", "visible");
                when = vWhen;
                backlog = JSON.parse(response.body);
                render();
                if(fn){
                    fn();
                }
                monitor.done();
            }
        });


    }

    function DropZone(id, backlogDiv){
        var v = $('<div id="dropZone' + id + '" class="drop-zone"></div>');


        function show(){
            v.css("visibility", "visible");
        }
        function hide(){
            v.css("visibility", "hidden");
        }

        function currentDropIsAcceptable(){
            return lastDragged.id !==id;
        }

        v.droppable({
            drop: function( event, ui ) {
                var subjectId = lastDragged.id;
                moveItemBefore(subjectId, id);
                hide();
            },
            over: function(event, ui){
                if(currentDropIsAcceptable()){
                    show();
                }
            },
            out: function(event, ui){
                hide();
            }
        });

        hide();
        v.appendTo(backlogDiv);
    }


    function EstimatesWidget(item, parentDiv){

        var v = $('<div id="' + item.id + '" class="estimates-list">' +
                '<div class="estimate"><select><option></option><option>swag</option><option>grooming</option><option>team</option></select> <input size="2" type="text"></div>' +
        '</div>');
        var view = {
                addButton : v.find(".add-button"),
                currencies : v.find("select"),
                value : v.find('input')
        };

        if(item.estimates && item.estimates.length > 0){
            const estimate = Util.findMostRecentEstimate(item);
            view.currencies.val(estimate.currency);
            view.value.val(estimate.value);
        }


        function getEstimateForCurrency(currency){
            var estimate;
            if(item.estimates){
                var matches = $.grep(item.estimates, function(estimate){
                    return estimate.currency === currency;
                });

                estimate = matches[0];
            }else{
                estimate = undefined;
            }

            return estimate;
        }

        var oldValue, oldCurrency;

        function onChange(){
            var currency, value;

            currency = view.currencies.val();
            value = view.value.val();

            if(value === ""){
                value = "0";
                view.value.val("0");
            }else if(value.length == 2 && value.indexOf("0") === 0){
                value = value.substring(1);
                view.value.val(value);
            }

            if(value!==oldValue || currency !==oldCurrency){

                oldValue = value;
                oldCurrency = currency;

                if(currency !== ""){
                    var estimate = getEstimateForCurrency(currency);


                    if(!estimate){
                        estimate = {id:uuid()};
                        if(!item.estimates){
                            item.estimates = [];
                        }
                        item.estimates.push(estimate);
                    }

                    estimate.currency = currency;
                    estimate.value = value;
                    estimate.when = getTime();

                    sendWorkInProgress();
                }
            }
        }

        view.currencies.bind("change", onChange);
        v.bind("keypress keydown keyup change", onChange);

        v.appendTo(parentDiv);

    }


    function ItemWidget(item, backlogDiv){
        var html, v, view, onDelete, showViewMode;
        const finishedCssClass = "finished";
        const unfinishedCssClass = "unfinished";

        v = $('<div id="' + item.id + '" class="item clearfix">' +
                '<img style="display:none;" src="/delete.png"/ class="delete-icon delete-button">' +
                '<img style="display:none;" src="/pencil.png"/ class="edit-icon edit-button">' +
                '<img style="display:none;" src="/medal.png"/ class="finished-icon finished-button">' +
                '<div class="controls" >' +
                '<div class="date-select-controls" style="display:none;">Target Date:<input type="text" class="date-picker" /></div><button style="display:none;" class="done-button">Done</button>' +
                '<div style="display:none;"class="estimates-holder"></div></div>' +
                '<span class="label"/>' + '<div class="remainder" style="display:none;"/>' +
                '<textarea style="display:none;"></textarea></div>');


        view = {
                label:v.find(".label"),
                remainder:v.find(".remainder"),
                textarea:v.find("textarea"),
                finishedButton:v.find(".finished-button"),
                editButton:v.find(".edit-button"),
                deleteButton:v.find(".delete-button"),
                doneButton:v.find(".done-button"),
                datePicker:v.find(".date-picker"),
                dateSelectControlsDiv:v.find(".date-select-controls"),
                estimatesHolder: v.find(".estimates-holder")
        };

        view.datePicker.datepicker().change(function(){
            item.when = view.datePicker.datepicker("getDate").getTime();
            sendWorkInProgress();
        });


        function mostRecentEstimateText(){

            var result;
            var mostRecentEstimate = Util.findMostRecentEstimate(item);

            if(mostRecentEstimate){
                result = "(" + mostRecentEstimate.value + " " + mostRecentEstimate.currency + ")";
            }else{
                result = "";
            }

            return result;
        }

        var showViewMode;



        function setText(text, decoration){
            var lines = text.split('\n');
            if(lines.length>0){
                var firstLine = lines[0];

                var label = firstLine;
                if(decoration){
                    label = label + " " + decoration;
                }

                view.label.text(label);

                if(lines.length>1){
                    var remainder = text.substring(firstLine.length);
                    view.remainder.text(remainder);
                }
            }
        }

        view.label.click(function(){
            if(view.remainder.css("display")==="none"){
                view.remainder.slideDown();
            }else{
                view.remainder.slideUp();
            }
        });

        function setStoryCompletionStateCssClass(){

            if(item.isComplete){
                v.addClass(finishedCssClass);
                v.removeClass(unfinishedCssClass);
            }else {
                v.removeClass(finishedCssClass);
                v.addClass(unfinishedCssClass);
            }
        }

        if(item.kind==="goal"){
            v.addClass("milestone divider clearfix");
            showViewMode = function(){
                var whenText = item.when ? (" (" + formatLongDateTime(item.when) + ")") : " (no date set)";
                setText("GOAL: " + item.name,  whenText);
            }
            if(item.when){
                view.datePicker.datepicker("setDate", new Date(item.when));
            }
        }else {
            if(item.kind==="story"){
                v.addClass("story project-chunk");
                setStoryCompletionStateCssClass();
            }else if(item.kind==="epic"){
                v.addClass("epic project-chunk");
            }
            showViewMode = function(){
                setText(item.name, mostRecentEstimateText());
            }
            EstimatesWidget(item, view.estimatesHolder);
        }



        showViewMode();

        view.textarea.val(item.name);
        view.textarea.bind("keypress change",function(n){
            item.name = view.textarea.val();
            view.label.text(item.name);
            sendWorkInProgress();
        });

        function makeDraggable(){

            v.draggable({
                start: function( event, ui ) {
                    lastDragged = item;
                },
                stop:function(event, ui){
                    ui.helper.css("left", 0);
                    ui.helper.css("top", 0);
                }
            });
        }

        function showEditableMode(){
            showViewMode();
            view.editButton.show();
            view.label.show();
            view.remainder.hide();
            if(!item.isComplete) {
                view.deleteButton.show();
            }
            if(item.kind==="story"){
                view.finishedButton.show();
            }

            view.dateSelectControlsDiv.hide();

            view.textarea.hide();
            view.doneButton.hide();
            view.estimatesHolder.hide();

            makeDraggable();
        }

        function showEditMode(){
            view.label.hide();
            view.remainder.hide();
            view.editButton.hide();
            view.deleteButton.hide();
            view.finishedButton.hide();


            if(item.kind==="goal"){
                view.dateSelectControlsDiv.show();
            }

            var textAreaWidth = window.innerWidth / 2,
                textAreaHeight = "25em";

            view.textarea.show();
            view.textarea.css({"width":textAreaWidth, "height": textAreaHeight});
            view.textarea.focus();
            view.doneButton.show();
            view.estimatesHolder.show();

            makeDraggable();
        }

        view.doneButton.button().click(showEditableMode);

        view.finishedButton.click(function(){

            item.isComplete = !item.isComplete;
            setStoryCompletionStateCssClass();

            sendWorkInProgress();
        });
        view.editButton.click(showEditMode);
        view.deleteButton.click(function(){
            deleteItem(item);
        });

        v.appendTo(backlogDiv);


        function scrollTo(){
            $('html, body').animate({
                scrollTop: v.offset().top + 'px'
            }, 'fast');
        }


        return {
            showEditMode:showEditMode,
            showEditableMode:showEditableMode,
            scrollTo:scrollTo,
            id:item.id
        };
    }

    function findItemNumById(id){
        var result;

        $.each(backlog.items, function(idx, item){
            if(item.id===id){
                result = idx;
            }

        });

        return result;
    }

    function findItemById(id){
        var matches = $.grep(backlog.items, function(item){
            const result = item.id===id;
            return result;
        });

        return matches[0];
    }

    function readView(){

        var newList = [];

        where.find(".project-chunk, .milestone").each(function(idx, domElement){
            var id = $(domElement).attr("id");
            var chunk = findItemById(id);

            newList.push(chunk);
        });
        backlog.items = newList;
        backlog.memo = view.commitMessage.val();
    }

    var slider = HistorySlider(view.slider);

    view.toggleBurndownButton.button().click(
        function () {
            $(".chart").slideToggle();
        }
    );


    // goals that have only finished items between them and the next goal
    function detectLeadingFinishedGoals(){
        var finishedGoals = [];
        var previousGoal = undefined;
        for(x=0;x<backlog.items.length;x++){
            var item = backlog.items[x];
            if(item.kind=="goal"){
                if(previousGoal){
                   finishedGoals.push(previousGoal);
                }
                previousGoal = item;
            } else if(item.kind!="story" || !item.isComplete){
                if(previousGoal) finishedGoals.push(previousGoal);
                break;
            }
        }
        return finishedGoals;
    }

    function toggleFinished(){

        var finishedStories, finishedGoals, relatedDropZones;

        function findRelatedDropZones(listOfDivs){
            return _.map(listOfDivs, function(i){return $("#dropZone" + $(i).attr('id'));});
        }

        function toggleEach(listOfJQueries){
            $.each(listOfJQueries, function (idx, i){
              i.slideToggle();
            });
        }

        finishedStories = $(".finished");
        finishedGoals = _.map(detectLeadingFinishedGoals(), function(goal){return $("#" + goal.id);});
        relatedDropZones = findRelatedDropZones(finishedStories).concat(findRelatedDropZones(finishedGoals));

        finishedStories.slideToggle();

        toggleEach(finishedGoals);
        toggleEach(relatedDropZones);

    }

    view.hideButton.button().click(toggleFinished);

    view.statsButton.button().click(
        function () {
            $(".details").slideToggle();
        }
    );

    view.editButton.button().click(function(){
        showWIPVersion(function(){
            view.commitMessage.val("");
            showEditMode();
            slider.showCurrent();
            view.memoTextArea.css("visibility", "hidden");
        });
    });

    view.saveButton.button().click(function(){
        readView();
        commitDialog.show();

    });



    /**
     * http://ejohn.org/blog/getboundingclientrect-is-awesome/
     */
    function isElementInViewport (el) {
        //special bonus for those using jQuery
        if (el instanceof jQuery) {
            el = el[0];
        }

        var rect = el.getBoundingClientRect();

        return (
            rect.top >= 0 &&
            rect.left >= 0 &&
            rect.bottom <= (window.innerHeight || document.documentElement.clientHeight) && /*or $(window).height() */
            rect.right <= (window.innerWidth || document.documentElement.clientWidth) /*or $(window).width() */
        );
    }
    function onlyVisible(items) {
        return items.filter(function(idx, i){
            return isElementInViewport(i) && $(i).is(":visible");
        });
    }


    function addNewItem(item){

        var idsOfItemsInView = onlyVisible($(".item")).map(function (){return $(this).attr("id");});

        function middleItem(array){
            return array[Math.floor(array.length /2)];
        }

        var middleId = middleItem(idsOfItemsInView);

        var middlePos = undefined;
        for (x=0;x<widgets.length;x++){
            var next = widgets[x];
            if(next.id == middleId) {
                middlePos = x;
            }
        }

         DropZone(item.id, view.backlog);
         const widget = ItemWidget(item, view.backlog);

         // jump through some hoops to move the thing to the middle of the page & backlog
         widgets.splice(middlePos, 0, widget);
         backlog.items.splice(middlePos, 0, item);

         var foo = $("#" + middleId);
         var dz = $("#dropZone" + item.id);
         dz.insertAfter(foo);
         $("#" + item.id).insertAfter(dz);

         widget.showEditMode();
         sendWorkInProgress();
    }


    view.addStoryButton.button().click(function(){
        addNewItem({
            id:uuid(),
            name:"",
            kind:"story"
        });
    });

    view.addEpicButton.button().click(function(){
        addNewItem({
            id:uuid(),
            name:"",
            kind:"epic"
        });
    });

    view.addGoalButton.button().click(function(){
        addNewItem({
            id:uuid(),
            name:"",
            kind:"goal"
        });
    });

    view.titleSpan.mouseover(function() {
        if (view.renameButton.data("state") == "on") {
            view.renameButton.show();
        }
    });

    view.titleSpan.mouseout(function() {
        view.renameButton.hide();
    });

    view.renameButton.button().click(function(){
        var newName = prompt("Rename the backlog", backlog.name);
        if (newName===null || !newName) return; // some name is required
        if (newName == backlog.name) return; // no change
        view.title.text(newName);
        backlog.name = newName;
        sendWorkInProgress();
    });

    function readIntegerOrUndefinedIfBlank(string){
        const text = string.trim();

        var WholeNumbers = /^\d+$/
            if(text === ""){
                return undefined;
            } else if(WholeNumbers.test(text)){
                return parseInt(text, 10);
            }else{
                throw "NOT A NUMBER: " + text;
            }

    }

    function handleVelocityInput(){

        try{
            var newVelocity = readIntegerOrUndefinedIfBlank(view.velocityTextField.val());
            if(newVelocity!==backlog.projectedVelocity){
                backlog.projectedVelocity = newVelocity;
                sendWorkInProgress();
            }

        }catch(err){
            view.velocityTextField.val(backlog.projectedVelocity);
        }

    }

    view.velocityTextField.keyup(handleVelocityInput);
    view.velocityTextField.change(handleVelocityInput);

    function showWIPVersion(fn){
        showLatestVersion(fn, true);
    }
    function showLatestVersion(fn, evenIfWIP){
        var monitor = activityMonitor.show();
        http({
            url: "/api/backlogs/" + backlogId + "/history?showLatestEvenIfWip=" + evenIfWIP,
            method: "GET",
            onResponse: function (response) {
                var history =JSON.parse(response.body);
                var latest = history[0];
                showVersion(latest.version, latest.when, function(){
                    if(fn){
                        fn();
                    }
                    monitor.done();
                });
            }
        });

    }

    function showCurrentVersion(fn){
        showLatestVersion(fn, false);
    }

    var chart = (function(){

        function render(when){
            var monitor = activityMonitor.show();
            var url = "/api/backlogs/" + backlogId + "/chart/" + globalConfig.defaultChartType + "?&showGoalTargetDots=true&showOddWeeks=false&showWeekNumbers=false&showMonthLabels&showCompletedWork&showGoalLabels=false&showMonthVerticals=true&showGoalHLines=false&showGoalVLines";

            if(when){
                url = url+"&end=" + when + "&showLatestEvenIfWip=true";
            }
            var height = $(".chart").height();

            if(height>30) $(".chart").css("height", height);

            $.ajax(url, {
                dataType:"text",
                success:function(data){
                    $(".chart").html(data);
                    monitor.done();
                }
            });

            $("a.permalink").attr("href", url);
        }

        function refresh(){
            render(getTime())
        }

        return {
            render:render,
            refresh:refresh
        }
    }());

    showCurrentVersion(function(){

        function goToByScroll(id, fn){
            var props = {scrollTop: $(id).offset().top - $('.chart').height()};
            $('html,body').animate(props,'slow', undefined, fn);
        }

        var hash = window.location.hash;

        if(hash) {
            $(".chart").hide();
            $(hash).css("border", "5px solid yellow");

            var scrollToHash = _.partial(goToByScroll, hash);

            scrollToHash(function(){
                // now scroll again, just in case the floating header is occluding our target
                var hackyTimeout = 1000; // <-- HACK!
                setTimeout(scrollToHash, hackyTimeout);
            });
        }else{
            toggleFinished();
        }

    });

});
