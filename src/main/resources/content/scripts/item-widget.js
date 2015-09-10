define([
    "jquery",
    "jqueryui",
    "react",
    "underscore",
    "util",
    "estimates-widget"
],
    function($, jqueryui, React, _, Util, EstimatesWidget) {

        return function(opts) {

            const finishedCssClass = "finished";
            const unfinishedCssClass = "unfinished";

            var item =        opts.item,
                container =   opts.container,
                onChange =    opts.onChange,
                onDelete =    opts.onDelete,
                onStartDrag = opts.onStartDrag,
                html,

                v = $(
                    '<div id="' + item.id + '" class="item">' +
                    '    <div class="row item-label-header">' +
                    '        <div class="small-9 columns">' +
                    '            <div class="status-view left"><i class="fa fa-fw status-icon"/></div>' +
                    '            <div class="item-label"/>' +
                    '        </div>' +
                    '        <div class="small-3 columns text-right">' +
                    '            <span class="goal-date right"/>' +
                    '            <div class="estimate-view right"></div>' +
                    '        </div>' +
                    '    </div>' +
                    '    <div class="row view-mode-header">' +
                    '        <div class="small-11 columns"></div>' +
                    '    </div>' +
                    '    <div class="row editable-mode-header" style="display:none;">' +
                    '        <div class="small-8 columns">' +
                    '        </div>' +
                    '        <div class="small-4 columns text-right">' +
                    '            <a href="#" class="in-progress-button" title="in progress"><i class="fa fa-chevron-right"></i></a> ' +
                    '            <a href="#" class="edit-button" title="edit"><i class="fa fa-pencil"></i></a> ' +
                    '            <a href="#" class="finished-button" title="completed"><i class="fa fa-check"></i></a> ' +
                    '            <a href="#" class="delete-button" title="delete"><i class="fa fa-remove"></i></a> ' +
                    '        </div>' +
                    '    </div>' +
                    '    <div class="row edit-mode-header" style="display:none;"></div>' +
                    '    <div class="row goal-edit-mode-header" style="display:none;">' +
                    '        <div class="small-4 columns">' +
                    '            <div class="row collapse">' +
                    '                <div class="small-3 columns">' +
                    '                    <span class="prefix">Target Date</span>' +
                    '                </div>' +
                    '                <div class="small-5 columns">' +
                    '                    <input type="text" class="date-picker" />'  +
                    '                </div>' +
                    '                <div class="small-2 end columns postfix-radius">' +
                    '                    <a href="#" class="button tiny postfix done-button">Done</a>' +
                    '                </div>' +
                    '            </div>' +
                    '        </div>' +
                    '    </div>' +
                    '    <div class="row remainder" style="display:none;"></div>' +
                    '    <div class="row entry" style="display:none;">' +
                    '        <div class="small-12 columns">' +
                    '            <textarea></textarea>' +
                    '        </div>' +
                    '    </div>' +
                    '</div>'
                ),

                view = {
                    itemLabelHeader:        v.find('.item-label-header'),
                    viewModeHeader:         v.find('.view-mode-header'),
                    editableModeHeader:     v.find('.editable-mode-header'),
                    editModeHeader:         v.find('.edit-mode-header'),

                    goalDateView:           v.find('.goal-date'),
                    goalEditModeHeader:     v.find('.goal-edit-mode-header'),
                    datePicker:             v.find('.date-picker'),
                    doneButton:             v.find('.done-button'),

                    label:                  v.find('.item-label'),
                    estimateView:           v.find('.estimate-view'),
                    estimate2:              v.find('.estimate2'),
                    statusView:             v.find('.status-view'),
                    statusIcon:             v.find('.status-icon'),

                    remainder:              v.find('.remainder'),
                    entry:                  v.find('.entry'),
                    textarea:               v.find('.entry textarea'),

                    finishedButton:         v.find('.finished-button'),
                    inProgressButton:       v.find('.in-progress-button'),
                    editButton:             v.find('.edit-button'),
                    deleteButton:           v.find('.delete-button')
                },

                log = function log() {
                    console.log.apply(console, arguments);
                },
                onItemWidgetChange = function onItemWidgetChange() {
                    log("onItemWidgetChange: ");
                    onChange();
                },

                showEditableMode = function showEditableMode() {
                    showViewMode();

                    view.viewModeHeader.hide();
                    view.editModeHeader.hide();
                    view.editableModeHeader.show();
                    view.goalEditModeHeader.hide();

                    view.deleteButton.toggle(item.isComplete!==true);
                    view.inProgressButton.toggleClass('in-progress-button-on', item.inProgress===true);
                    view.inProgressButton.toggle(item.kind==="story" && item.isComplete!==true);
                    view.editButton.toggle(item.isComplete!==true);

                    if((item.kind==="goal")) {
                        view.inProgressButton.hide();
                    }

                    view.remainder.hide();
                    view.entry.hide();

                    makeDraggable();
                },

                showEditMode = function showEditMode() {

                    view.viewModeHeader.hide();
                    view.editableModeHeader.hide();
                    if(item.kind==="goal") {
                        view.goalEditModeHeader.show();
                        view.datePicker.val(Util.formatLongDateTime(item.when));
                    }
                    else { // story and epic
                        view.editModeHeader.show();
                    }

                    view.remainder.hide();

                    // TODO: dynamically figure out a height
                    // var textAreaYPosition = the position of the top of the text area in the viewport
                    // var totalViewportHeight = height of the viewport
                    // var newHeight = totalVieewportHeight - textAreaYPosition

                    var textAreaHeight = "25em";
                    view.textarea.css({height:textAreaHeight});

                    view.entry.show();
                    view.textarea.focus();

                    view.itemLabelHeader.off('click');
                    makeDraggable();
                },

                mostRecentEstimateText = function mostRecentEstimateText() {
                    var result;
                    var mostRecentEstimate = Util.findMostRecentEstimate(item);
                    if (mostRecentEstimate) {
                        result =  mostRecentEstimate.value + " " + mostRecentEstimate.currency;
                    }
                    else {
                        result = "";
                    }
                    return result;
                },

                makeDraggable = function makeDraggable() {
                    v.draggable({
                        start: function() {
                            onStartDrag(item);
                        },
                        stop: function(event, ui) {
                            ui.helper.css("left", 0);
                            ui.helper.css("top",  0);
                        }
                    });
                },

                setStatusCss = function setStatusCss() {
                    v.toggleClass(finishedCssClass, item.isComplete===true);
                    v.toggleClass(unfinishedCssClass, item.isComplete!==true);
                },

                scrollTo = function scrollTo() {
                    $('html, body').animate({
                        scrollTop: v.offset().top + 'px'
                    }, 'fast');
                },

                getFirstLine = function getFirstLine(text) {
                    var lines = text.split('\n');
                    var first = ""
                    if (lines.length > 0) {
                        first = lines[0];
                    }

                    return first;
                },

                setText = function setText(text, decoration) {
                    var lines = text.split('\n');
                    if (lines.length > 0) {
                        var firstLine = lines[0];
                        var label = firstLine;
                        if(decoration) {
                            label = label + " " + decoration;
                        }
                        view.label.text(label);
                        if(lines.length > 1) {
                            var remainder = text.substring(firstLine.length);
                            view.remainder.text(remainder.trim());
                        }
                    }
                },

                toggleRemainder = function toggleRemainder() {
                    if (view.remainder.css("display")==="none") {
                        view.remainder.slideDown();
                        view.label.toggleClass('expanded', true);

                    }
                    else {
                        view.remainder.slideUp();
                        view.label.toggleClass('expanded', false);
                    }
                },

                setStatusView = function setStatusView() {
                    var allIcons = 'fa-check fa-chevron-right fa-trophy';
                    if (item.isComplete===true) {
                        view.statusIcon.removeClass(allIcons).addClass('fa-check');
                        view.statusView.css('visibility', 'visible');
                    }
                    else if (item.inProgress===true) {
                        view.statusIcon.removeClass(allIcons).addClass('fa-chevron-right');
                        view.statusView.css('visibility', 'visible');
                    }
                    else {
                        view.statusIcon.removeClass(allIcons);
                        view.statusView.css('visibility', 'hidden');
                    }
                },

                toggleGoalLabel = function toggleGoalLabel() {
                    console.log('toggleGoalLabel');
                    view.label.toggleClass('expanded');
                },

                setupHandlers = function() {
                    view.viewModeHeader.click(toggleRemainder);

                    view.textarea.val(item.name);
                    view.textarea.bind("change keydown keyup", function() {
                        item.name = view.textarea.val();
                        view.label.text(getFirstLine(item.name));
                        onChange();
                    });

                    view.doneButton.click(showEditableMode);

                    view.editButton.click(function(ev) {
                        ev.stopPropagation();
                        showEditMode();
                    });

                    view.finishedButton.click(function(ev) {
                        ev.stopPropagation();
                        item.isComplete = item.isComplete !== true;
                        setStatusCss();
                        setStatusView();
                        onChange();
                        showEditableMode();
                    });

                    view.inProgressButton.click(function(ev) {
                        ev.stopPropagation();
                        item.inProgress = item.inProgress !== true;
                        view.inProgressButton.toggleClass('in-progress-button-on', item.inProgress);
                        setStatusCss();
                        setStatusView();
                        onChange();
                    });

                    view.deleteButton.click(function(ev) {
                        ev.stopPropagation();
                        //DeleteConfirmation(container, item, onDelete);
                        var deleteConfirmButton = $('.delete-confirm-button'),
                            deleteCancelButton  = $('.delete-cancel-button');

                        deleteConfirmButton.click(function() {
                            onDelete(item);
                            $('#delete-modal').foundation('reveal', 'close');
                        });
                        deleteCancelButton.click(function() {
                            $('#delete-modal').foundation('reveal', 'close');
                        });
                        $('#delete-modal').foundation('reveal', 'open');
                    });

                    view.datePicker.datepicker().change(function() {
                        item.when = view.datePicker.datepicker("getDate").getTime();
                        onChange();
                    });
                },

                setupGoal = function setupGoal() {
                    v.addClass("milestone divider cfix");
                    showViewMode = function() {
                        setStatusView();

                        var whenText = item.when
                            ? ("" + Util.formatLongDateTime(item.when) + "")
                            : "[no date set]";
                        setText("GOAL: " + item.name);
                        view.goalDateView.text(whenText);
                        view.goalDateView.show();

                        view.viewModeHeader.show();
                        view.editableModeHeader.hide();
                        view.editModeHeader.hide();
                        view.goalEditModeHeader.hide();
                        view.itemLabelHeader.off('click').click(toggleGoalLabel);
                    };

                    if(item.when) {
                        view.datePicker.datepicker("setDate", new Date(item.when));
                    }
                },

                setupStory = function setupStory() {
                    v.addClass("story");
                },

                setupEpic = function setupEpic() {
                    v.addClass("epic");
                },

                setEstimateView = function setEstimateView() {
                    var estimateText = mostRecentEstimateText();
                    var toggleSetting = estimateText==="";
                    if (toggleSetting) {
                        estimateText = "No Estimate!!!";
                    }
                    view.estimateView.toggleClass('no-estimate', toggleSetting);
                    view.estimateView.text(estimateText);
                },

                setupProjectChuck = function setupProjectChunk() {
                    showViewMode = function() {
                        v.addClass('project-chunk');
                        setText(item.name);

                        setEstimateView();
                        setStatusCss();
                        setStatusView();
                        view.goalDateView.hide();
                        view.itemLabelHeader.off('click').click(toggleRemainder);

                        view.viewModeHeader.show();
                        view.editableModeHeader.hide();
                        view.editModeHeader.hide();
                    };
                    EstimatesWidget(item, view.editModeHeader, onItemWidgetChange, showEditableMode);
                },

                setupItem = function setupItem() {
                    if(item.kind==="goal") {
                        setupGoal();
                    }
                    else {
                        if (item.kind==="story") {
                            setupStory();
                        }
                        else if (item.kind==="epic") {
                            setupEpic();
                        }
                        setupProjectChuck();
                    }
                };

            log('setting up');
            setupItem();
            setupHandlers();
            showViewMode();
            v.appendTo(container);

            return {
                showEditMode: showEditMode,
                showEditableMode: showEditableMode,
                scrollTo: scrollTo,
                id: item.id
            };
        }
    });
