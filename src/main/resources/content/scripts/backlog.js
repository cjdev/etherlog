define([
    "jquery",
    "jqueryui",
    "react",
    "underscore",
    "http",
    "uuid",
    "util",
    "item-widget",
    "activity-monitor",
    "history-slider",
    "authUtil",
   // "jsx!backlog-statistics",
    "modernizr",
    "fastclick",
    "foundation.reveal",
    "foundation.slider"],
    function($, jqueryui, React, _, http, uuid, Util, ItemWidget, ActivityMonitor, HistorySlider, authUtil/*, BacklogStats*/) {
        
    
        $(document).foundation();

        authUtil.redirectToLoginIfNotLoggedIn();
        $(".logout-button").click(function(){
            authUtil.logout();
        });
        
        const kindsInOrderOfPrecedence = ["team", "grooming", "swag"];
        const _DEBUG = true;

        function log() {
            if (_DEBUG) {
                console.log.apply(console, arguments);
            }
        }
        var globalConfig,
            backlog,
            lastDragged,
            when,
            latestVersion,
            backlogId = parseBacklogIdFromURL(),
            widgets = [],
            where = $("body"),
            view = {
                homeButton: where.find('.home-button'),
                backlogName: where.find('#backlog-name'),
                backlog : where.find(".backlog"),
                velocityView : where.find('.current-velocity'),
                slider : where.find("#slider"),
                summaryTextArea : where.find("#summary"),
                chartToggleButton: where.find(".chart-toggle-button"),
                chartPanel: where.find(".chart-panel"),
                statsToggleButton: where.find(".stats-toggle-button"),
                statsPanel: where.find(".stats-panel"),
                statsContent: where.find(".stats-panel .stats-content"),
                finishedButton: where.find(".finished-toggle-button"),
                editButton : where.find(".edit-backlog-button"),
                publishButton: where.find(".publish-button"),
                publishModal: where.find('#publish-modal'),
                publishSummary: where.find('#publish-modal .summaries'),
                commitMessage : where.find(".commit-message"),
                publishCancelButton: where.find('.publish-cancel-button'),
                publishConfirmButton: where.find(".publish-confirm-button"),
                addStoryButton : where.find(".add-story-button"),
                addEpicButton : where.find(".add-epic-button"),
                addGoalButton : where.find(".add-goal-button"),
                settingsMenu: where.find(".settings-menu"),
                renameButton: where.find(".rename-backlog-button"),
                renameModal : where.find("#rename-modal"),
                renameInput : where.find(".rename-input"),
                renameConfirmButton: where.find(".rename-confirm-button"),
                renameCancelButton: where.find(".rename-cancel-button"),
                velocityButton: where.find(".velocity-button"),
                velocityModal: where.find("#velocity-modal"),
                velocityInput: where.find(".velocity-input"),
                velocityConfirmButton: where.find(".velocity-confirm-button"),
                velocityCancelButton: where.find(".velocity-cancel-button"),
                memoTextArea : where.find(".memo-text")
            },

            activityMonitor = ActivityMonitor(view.homeButton,
                function() {view.homeButton.addClass("fa-spin")},
                function() {view.homeButton.removeClass("fa-spin")});

        function parseBacklogIdFromURL(){
            var parts = window.location.toString().split("#")[0].split("/");
            if(parts.length>0){
                return parts[parts.length-1];
            }else{
                return undefined;
            }
        }

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
                                backlog.optimisticLockVersion =
                                    JSON.parse(response.body).optimisticLockVersion;
                                updateSummary();
                                updateStats();
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
            lastChange = Util.getTime();
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
            return summarize(changes.added, "Added Stories/Epics") +
                summarize(changes.removed, "Removed Stories/Epics") +
                summarize(changes.finished, "Completed stories") +
                summarize(changes.reopened, "Reopened stories") +
                summarize(changes.reestimated, "re-estimated stories/epics");
        }

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

        function render(){
            view.velocityView.text(backlog.projectedVelocity);
            view.velocityInput.val(backlog.projectedVelocity);
            view.memoTextArea.text("Last change "
                + Util.formatLongDateTime(when?when:Util.getTime())
                + ": "
                + backlog.memo);

            view.backlogName.text(backlog.name);
            view.backlog.empty();

            $.each(backlog.items, function(idx, item){
                DropZone(item.id, view.backlog);
                widgets.push(ItemWidget({
                    item: item,
                    container: view.backlog,
                    onChange: sendWorkInProgress,
                    onDelete: deleteItem,
                    onStartDrag: setLastDragged
                }));
            });
            updateStats();
            updateSummary();
            chart.render(when);
        }

        function updateStats() {
            /*React.render(React.createElement(BacklogStats, {backlog: backlog}), view.statsContent.get(0));*/
        }

        function updateSummary(){
            var itemsNotDone = _.filter(backlog.items, function(item){return !item.isComplete;});
            var itemsDone = _.filter(backlog.items, function(item){return item.isComplete;});

            function printStuff(stuff){
                return $.map(stuff, function(value, key){return key + " " + value + "  ";})
            }
            var totalsTodo = calculateTotals(itemsNotDone);

            var changesSinceLastPublishedVersion = fetchChanges("in-" + backlog.optimisticLockVersion);

            var summaryText = "";
            if(changesSinceLastPublishedVersion){
                var averageVelocityText = "";
                var weekSpans = [2, 6, 12];
                _.each(weekSpans, function(numWeeks){
                    var nWeeksAgo = Util.formatLongDateTime(when - (1000 * 60 * 60* 24 * 7 * numWeeks));
                    var changesOverPastNWeeks = fetchChanges("from-" + nWeeksAgo + "-to-" + Util.formatLongDateTime(when));
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
            view.editButton.hide();
            view.settingsMenu.show();
            view.commitMessage.show();
            view.publishButton.show();
            view.addStoryButton.show();
            view.addEpicButton.show();
            view.addGoalButton.show();
        }

        function showViewMode(){
            view.settingsMenu.hide();
            view.editButton.show();
            view.publishButton.hide();
            view.addStoryButton.hide();
            view.addEpicButton.hide();
            view.addGoalButton.hide();
            render();
        }

        function showVersion(version, vWhen, fn){
            var monitor = activityMonitor.show();

            http({
                url:"/api/backlogs/" + backlogId + "/history/" + version,
                method:"GET",
                onResponse:function(response){
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

        function setLastDragged(item) {
            lastDragged = item;
        }

        function DropZone(id, backlogDiv){

            var v = $('<div id="dropZone' + id + '" class="drop-zone"></div>');

            function show(){
                //v.addClass("on");
                v.css("visibility", "visible");
            }
            function hide(){
                //v.removeClass('on');
                v.css("visibility", "hidden");
            }

            function currentDropIsAcceptable(){
                return lastDragged.id !==id;
            }

            v.droppable({
                drop: function() {
                    var subjectId = lastDragged.id;
                    moveItemBefore(subjectId, id);
                    hide();
                },
                over: function(){
                    if(currentDropIsAcceptable()){
                        show();
                    }
                },
                out: function(){
                    hide();
                }
            });

            hide();
            v.appendTo(backlogDiv);
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
                return item.id === id;
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

        // goals that have only finished items between them and the next goal
        function detectLeadingFinishedGoals(){
            var finishedGoals = [];
            var previousGoal = undefined;
            for(var x=0;x<backlog.items.length;x++){
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

        function hideFinished() {
            toggleFinished(false);
        }

        function toggleFinished(showing) {
            var finished, finishedGoals, relatedDropZones;

            view.finishedButton.parent().toggleClass("active", showing);

            function findRelatedDropZones(listOfDivs){
                return _.map(listOfDivs, function(i){return $("#dropZone" + $(i).attr('id'));});
            }

            function toggleEach(listOfJQueries, directionDown){
                $.each(listOfJQueries, function (idx, i){
                    if (directionDown) { i.slideDown(); }
                    else { i.slideUp();}
                });
            }

            finished = $(".item.finished");
            if (showing) {
                finished.slideDown();
            }
            else {
                finished.slideUp();
            }

            finishedGoals = _.map(detectLeadingFinishedGoals(), function(goal){return $("#" + goal.id);});
            relatedDropZones = findRelatedDropZones(finished).concat(findRelatedDropZones(finishedGoals));
            toggleEach(finishedGoals, showing);
            toggleEach(relatedDropZones, showing);
        }

        /**
         * http://ejohn.org/blog/getboundingclientrect-is-awesome/
         */
        function isElementInViewport (el) {
            //special bonus for those using jQuery
            if (el instanceof jQuery) {
                el = el[0];
            }

            var rect = el.getBoundingClientRect();

            return (rect.top >= 0 &&
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
            var middlePos, middleId;

            function middleItem(array){
                return array[Math.floor(array.length /2)];
            }

            var idsOfItemsInView = onlyVisible($(".item")).map(function (){return $(this).attr("id");});
            middleId = middleItem(idsOfItemsInView);
            middlePos = undefined;
            for (var x=0;x<widgets.length;x++){
                var next = widgets[x];
                if(next.id == middleId) {
                    middlePos = x;
                }
            }

            DropZone(item.id, view.backlog);
            const widget = ItemWidget({
                item: item,
                container: view.backlog,
                onChange: sendWorkInProgress,
                onDelete: deleteItem,
                onStartDrag: setLastDragged
            });

            // jump through some hoops to move the thing to the middle of the page & backlog
            widgets.splice(middlePos, 0, widget);
            backlog.items.splice(middlePos, 0, item);

            var middleElem = $("#" + middleId);
            var dropzoneElem = $("#dropZone" + item.id);
            dropzoneElem.insertAfter(middleElem);
            $("#" + item.id).insertAfter(dropzoneElem);

            widget.showEditMode();
            sendWorkInProgress();
        }

        function readIntegerOrUndefinedIfBlank(string){
            const text = string.trim();

            var WholeNumbers = /^\d+$/;
            if(text === ""){
                return undefined;
            } else if(WholeNumbers.test(text)){
                return parseInt(text, 10);
            }else{
                throw "NOT A NUMBER: " + text;
            }
        }

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

        function handleVelocityInput(){
            try{
                var newVelocity = readIntegerOrUndefinedIfBlank(view.velocityInput.val());
                if(newVelocity!==backlog.projectedVelocity){
                    backlog.projectedVelocity = newVelocity;
                    sendWorkInProgress();
                    view.velocityView.text(newVelocity);
                    return true;
                }

            }catch(err){
                view.velocityInput.val(backlog.projectedVelocity);
            }
            return false;
        }

        var chart = (function(){
            function render(when){
                var monitor = activityMonitor.show();
                var url = "/api/backlogs/" + backlogId + "/chart/" + globalConfig.defaultChartType
                    + "?&showGoalTargetDots=true&showOddWeeks=false&showWeekNumbers=false&showMonthLabels&showCompletedWork&showGoalLabels=false&showMonthVerticals=true&showGoalHLines=false&showGoalVLines";

                if(when){
                    url = url+"&end=" + when + "&showLatestEvenIfWip=true";
                }
                var chartContainer = $(".chart");

                $.ajax(url, {
                    dataType:"text",
                    success:function(data){
                        chartContainer.html(data);
                        monitor.done();
                    }
                });

                $("a.permalink").attr("href", url);
            }

            function refresh(){
                render(Util.getTime())
            }

            return {
                render:render,
                refresh:refresh
            }
        }());

        view.backlogName.click(function() {
            window.location.reload();
        });

        view.publishButton.hide();
        view.addStoryButton.hide();
        view.addEpicButton.hide();
        view.addGoalButton.hide();
        view.settingsMenu.hide();

        var lastServerUpdate = Util.getTime();
        var lastChange = lastServerUpdate;
        var slider = HistorySlider(view.slider, backlogId, showVersion);


        function resetChartMenu() {
            view.chartPanel.slideUp();
            view.chartToggleButton.parent().removeClass('active');
        }

        function resetStatsMenu() {
            view.statsPanel.slideUp();
            view.statsToggleButton.parent().removeClass('active');
        }

        view.chartToggleButton.click(function () {
            resetStatsMenu();
            view.chartPanel.slideToggle();
            view.chartToggleButton.parent().toggleClass('active');
        });

        view.statsToggleButton.click(function() {
            resetChartMenu();
            view.statsPanel.slideToggle();
            view.statsToggleButton.parent().toggleClass('active');
        });

        view.finishedButton.click(function() {
            view.finishedButton.parent().toggleClass('active');
            var showing = view.finishedButton.parent().hasClass('active');
            toggleFinished(showing);
        });

        view.editButton.click(function(){
            showWIPVersion(function(){
                view.commitMessage.val("");
                showEditMode();
                slider.showCurrent();
                hideFinished();
            });
        });

        view.addStoryButton.click(function(){
            addNewItem({
                id:uuid(),
                name:"",
                kind:"story"
            });
        });

        view.addEpicButton.click(function(){
            addNewItem({
                id:uuid(),
                name:"",
                kind:"epic"
            });
        });

        view.addGoalButton.click(function(){
            addNewItem({
                id:uuid(),
                name:"",
                kind:"goal"
            });
        });

        view.renameButton.click(function() {
            view.renameInput.val(backlog.name).select();
            view.renameModal.foundation('reveal', 'open');
        });

        view.renameCancelButton.click(function() {
            view.renameModal.foundation('reveal', 'close');
        });

        view.renameConfirmButton.click(function(){
            var newName = view.renameInput.val();
            if (newName===null || !newName) return; // some name is required
            if (newName == backlog.name) return; // no change
            view.backlogName.text(newName);
            backlog.name = newName;
            sendWorkInProgress();
            view.renameModal.foundation('reveal', 'close');
        });

        view.velocityButton.click(function() {
            view.velocityInput.val(backlog.projectedVelocity).select();
            view.velocityModal.foundation('reveal', 'open');
        });

        view.velocityCancelButton.click(function() {
            view.velocityModal.foundation('reveal', 'close');
        });


        view.velocityConfirmButton.click(function(){
            if (handleVelocityInput()) {
                view.velocityModal.foundation('reveal', 'close');
            }
        });

        view.publishButton.click(function() {
            http({
                url: "/api/backlogs/" + backlogId + "/deltas/since-last-published",
                method: "GET",
                onResponse: function (response) {
                    var changes = JSON.parse(response.body);
                    view.publishSummary.text(renderChanges(changes));
                    view.publishModal.foundation('reveal', 'open');
                }
            });
        });

        // hooking into the reveal modal opened event to set focus
        // on the publish-modal commit message, as the focus would
        // not work on the publishButton click handler.
        $(document).on('opened.fndtn.reveal', '[data-reveal]', function () {
            if($(this).attr('id')==='publish-modal') {
                view.commitMessage.focus();
            }
        });

        view.publishConfirmButton.click(function(){
            var memo = view.commitMessage.val();
            // TODO: Add some basic memo validation here, 'cause empty memos are a pain
            backlog.memo = memo;
            http({
                url: "/api/backlogs/" + backlogId,
                method: "PUT",
                data:JSON.stringify(backlog),
                onResponse: function (response) {
                    showViewMode();
                    slider.refresh();
                    window.location.reload(); // HACK!  TODO: remove this when you figure out the "chart doesn't refresh after saving" bug
                }
            });
        });

        view.publishCancelButton.click(function() {
            view.publishModal.foundation('reveal', 'close');
        });

        http({
            url: "/api/config",
            method: "GET",
            onResponse: function (response) {
                globalConfig = JSON.parse(response.body);
            }
        });

        http({
            url: "/api/backlogs",
            method: "GET",
            onResponse: function (response) {
                var backlogs = JSON.parse(response.body);
                var backlog = _.findWhere(backlogs, {id: backlogId});
                view.backlogName.text(backlog.name);
            }
        });

        // "main" ------------------------------------------------------------------
        showCurrentVersion(function(){

            function goToByScroll(id, fn){
                var props = {scrollTop: $(id).offset().top};
                $('html,body').animate(props,'slow', undefined, fn);
            }

            var hash = window.location.hash;

            if(hash) {
                $(hash).addClass('highlighted');

                var scrollToHash = _.partial(goToByScroll, hash);

                scrollToHash(function(){
                    // now scroll again, just in case the floating header is occluding our target
                    var hackyTimeout = 1000; // <-- HACK!
                    setTimeout(scrollToHash, hackyTimeout);
                });
            }else{
                hideFinished();
            }
        });

        sendUpdate();
    });

