define(["jquery", "jqueryui", "http", "uuid"], function($, jqueryui, http, uuid){

    const kindsInOrderOfPrecedence = ["team", "grooming", "swag"];

    var backlog, where, lastDragged;

    var when; // SUPERHACK!

    var backlogId = parseBacklogIdFromURL();

    function parseBacklogIdFromURL(){
        var parts = window.location.toString().split("#")[0].split("/");
        if(parts.length>0){
            return parts[parts.length-1];
        }else{
            return undefined;
        }
    }

    function goToByScroll(id){
        $('html,body').animate({scrollTop: $(id).offset().top - $('img.chart').height()},'slow');
    }
    var hash = window.location.hash;
    console.log("hash is " + hash);
    if(hash) {
        setTimeout(function(){
            goToByScroll(hash);
            goToByScroll(hash);
        },800);
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
            title : where.find("#title"),
            backlog : where.find(".backlog"),
            slider : where.find("#slider"),
            summaryTextArea : where.find("#summary"),
            hideButton : where.find(".hide-button"),
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

    var lastServerUpdate = new Date().getTime();
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
                            updateSummary();
                            setTimeout(sendUpdate, 1000);
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
        lastChange = new Date().getTime();
    }


    function calculateTotals(backlog){
        var totals = {};

        $.each(kindsInOrderOfPrecedence, function(idx, kind){
            totals[kind] = 0;
        });

        function findBestEstimate(backlogItem){
            var bestEstimate;
            var estimates = backlogItem.estimates;
            if(estimates){
                $.each(kindsInOrderOfPrecedence, function(idx, kind){

                    $.each(estimates, function(idx, estimate){
                        if(!bestEstimate && estimate.currency === kind){
                            bestEstimate = {type:estimate.currency, value:parseInt(estimate.value, 10)};
                        }
                    });
                });
            }
            return bestEstimate;
        }

        $.each(backlog.items, function(idx, item){
            var bestEstimate = findBestEstimate(item);
            if(bestEstimate){
                totals[bestEstimate.type] = totals[bestEstimate.type] + bestEstimate.value;
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
        view.memoTextArea.text(formatLongDateTime(when?when:new Date().getTime()) + ": " + backlog.memo);
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
        var totals = calculateTotals(backlog);
        view.summaryTextArea.text("(" + $.map(totals, function(value, key){return key + " " + value + "  ";}) + ")");
    }

    function showEditMode(){

        $.each(widgets, function(idx, widget){
            widget.showEditableMode();
        });

        view.editButton.hide();
        view.commitMessage.show();
        view.saveButton.show();
        view.addStoryButton.show();
        view.addEpicButton.show();
        view.addGoalButton.show();
        view.velocityDiv.show();
    }

    function showViewMode(){
        view.memoTextArea.css("visibility", "visible");
        view.editButton.show();
        view.commitMessage.hide();
        view.saveButton.hide();
        view.addStoryButton.hide();
        view.addEpicButton.hide();
        view.addGoalButton.hide();
        view.velocityDiv.hide();
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
                            console.log(ui.value + " " + i.version + "(" + i.memo + ") on " + i.when);
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
                console.log(lastDragged.name + " was dropped on " + id);
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
                '<div class="estimate"><select><option></option><option>swag</option><option>grooming</option><option>team</option></select> <input size="2" type="text"></input>   </div>' +
        '</div>');
        var view = {
                addButton : v.find(".add-button"),
                currencies : v.find("select"),
                value : v.find('input')
        };

        if(item.estimates && item.estimates.length > 0){
            const estimate = item.estimates[item.estimates.length-1];
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
                    estimate.when = new Date().getTime();

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
            console.log("when changed to " + new Date(item.when) + " \n" + JSON.stringify(item));
            sendWorkInProgress();
        });

        function compareEstimatesByWhen(a, b){
            if(a.when === b.when){
                return 0;
            }else if (a.when > b.when){
                return 1;
            }else{
                return -1;
            }
        }

        function mostRecentEstimateText(){
            var result;

            if(item.estimates && item.estimates.length>0){
                item.estimates.sort(compareEstimatesByWhen);
                const mostRecentEstimate = item.estimates[item.estimates.length-1];
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

            view.textarea.show();
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
            scrollTo:scrollTo
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

    view.hideButton.button().click(
        function () {
            $(".finished").slideToggle();
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

        http({
            url: "/api/backlogs/" + backlogId,
            method: "PUT",
            data:JSON.stringify(backlog),
            onResponse: function (response) {
                showViewMode();
                slider.refresh();
            }
        });

    });


    function addNewItem(item){

        DropZone(item.id, view.backlog);
        const widget = ItemWidget(item, view.backlog);
        widget.showEditMode();
        widgets.push(widget);
        backlog.items.push(item);

        widget.scrollTo();

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

    function readIntegerOrUndefinedIfBlank(string){
        const text = string.trim();

        var WholeNumbers = /^\d+$/
            if(text === ""){
                return undefined;
            } else if(WholeNumbers.test(text)){
                console.log(text + " is a number");
                return parseInt(text, 10);
            }else{
                throw "NOT A NUMBER: " + text;
            }

    }

    function handleVelocityInput(){

        try{
            var newVelocity = readIntegerOrUndefinedIfBlank(view.velocityTextField.val());
//          console.log("new velocity:" + newVelocity);

            if(newVelocity!==backlog.projectedVelocity){
//              console.log("Updating velocity velocity: " + newVelocity);
                backlog.projectedVelocity = newVelocity;
                sendWorkInProgress();
            }

        }catch(err){
//          console.log("Setting back to " + backlog.projectedVelocity);
            view.velocityTextField.val(backlog.projectedVelocity);
            //alert(err);
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
            var url = "/api/backlogs/" + backlogId + "/chart?&showGoalTargetDots=true&showOddWeeks=false&showWeekNumbers=false&showMonthLabels&showCompletedWork&showGoalLabels=false&showMonthVerticals=true&showGoalHLines=false&showGoalVLines";
            
            if(when){
                url = url+"&end=" + when + "&showLatestEvenIfWip=true";
            }

            $("img.chart").attr("src", url);
            $("a.permalink").attr("href", url);
            monitor.done();
        }

        function refresh(){
            render(new Date().getTime())
        }

        return {
            render:render,
            refresh:refresh
        }
    }());

    showCurrentVersion();


});
