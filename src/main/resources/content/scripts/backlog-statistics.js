define([
    "jquery",
    "underscore",
    "util"
],
    function($, _, Util) {
        return function(container) {

            var view,
                v = $(
                    '<div class="stats-wrapper">' +
                        '<div class="small-4 columns">' +

                        '    <div class="kind stories">' +
                        '        <div class="row header">' +
                        '            <div class="small-12 columns"><h5>Stories</h5></div>' +
                        '        </div>' +
                        '        <div class="row titles">' +
                        '            <div class="small-3 columns text-right empty">&nbsp;</div>' +
                        '            <div class="small-2 columns text-right todo">Todo</div>' +
                        '            <div class="small-2 columns text-right in-progress">In Progress</div>' +
                        '            <div class="small-2 columns text-right finished">Finished</div>' +
                        '            <div class="small-2 columns text-right total end">Total</div>' +
                        '        </div>' +
                        '    </div>' +
                        '</div>' +

                        '<div class="small-4 columns">' +
                        '    <div class="kind epics">' +
                        '        <div class="row header">' +
                        '            <div class="small-12 columns"><h5>Epics</h5></div>' +
                        '        </div>' +
                        '        <div class="row titles">' +
                        '            <div class="small-3 columns text-right empty">&nbsp;</div>' +
                        '            <div class="small-2 columns text-right todo">Todo</div>' +
                        '            <div class="small-2 columns text-right in-progress">In Progress</div>' +
                        '            <div class="small-2 columns text-right finished">Finished</div>' +
                        '            <div class="small-2 columns text-right total end">Total</div>' +
                        '        </div>' +
                        '    </div>' +
                        '</div>' +


                        '<div class="small-4 columns">' +
                        '    <div class="kind goals">' +
                            '    <div class="row header">' +
                            '        <div class="small-12 columns"><h5>Goals</h5></div>' +
                            '    </div>' +
                        '    </div>' +
                        '</div>' +
                    '</div>'
                );


            function createRow(cssClass, title) {
                var row = $(
                '    <div class="row '+cssClass+'">' +
                    '        <div class="small-3 columns ">'+title+'</div>' +
                    '        <div class="small-2 columns text-right todo">' +
                    '            <span class="count">-</span> / <span class="points">-</span>' +
                    '        </div>' +
                    '        <div class="small-2 columns text-right in-progress">' +
                    '            <span class="count">-</span> / <span class="points">-</span>' +
                    '        </div>' +
                    '        <div class="small-2 columns text-right finished">' +
                    '            <span class="count">-</span> / <span class="points">-</span>' +
                    '        </div>' +
                    '        <div class="small-2 columns text-right total end">' +
                    '            <span class="count">-</span> / <span class="points">-</span>' +
                    '        </div>' +
                    '    </div>'
                );
                return row;
            }

            var stories = v.find('.stories');
            stories.append(createRow('swag', 'Swag'));
            stories.append(createRow('grooming', 'Grooming'));
            stories.append(createRow('team', 'Team'));
            stories.append(createRow('unestimated', 'Unestimated'));
            stories.append(createRow('total', 'Totals'));

            var epics = v.find('.epics');
            epics.append(createRow('swag', 'Swag'));
            epics.append(createRow('grooming', 'Grooming'));
            epics.append(createRow('team', 'Team'));
            epics.append(createRow('unestimated', 'Unestimated'));
            epics.append(createRow('total', 'Totals'));


            view = {
                storiesSwag: v.find('.stories .swag'),
                storiesGrooming: v.find('.stories .grooming'),
                storiesTeam: v.find('.stories .team'),
                storiesUnestimated: v.find('.stories .unestimated'),
                storiesTotal: v.find('.stories .total'),

                epicsSwag: v.find('.epics .swag'),
                epicsGrooming: v.find('.epics .grooming'),
                epicsTeam: v.find('.epics .team'),
                epicsUnestimated: v.find('.epics .unestimated'),
                epicsTotal: v.find('.epics .total')


            };

            function log() {
                //console.log.apply(console, arguments);
            }

            function renderCell(cell, stat) {
                cell.find('.count').text(stat.count);
                cell.find('.points').text(stat.points);
            }

            function renderRow(viewPart, statsPart) {
                renderCell(viewPart.find('.todo') ,statsPart.todo);
                renderCell(viewPart.find('.in-progress'), statsPart.inProgress);
                renderCell(viewPart.find('.finished'), statsPart.finished);
                renderCell(viewPart.find('.total'), statsPart.total);
            }

            function render(stats) {
                log('render stats....');

                renderRow(view.storiesSwag, stats.story.swag);
                renderRow(view.storiesGrooming, stats.story.grooming);
                renderRow(view.storiesTeam, stats.story.team);
                renderRow(view.storiesUnestimated, stats.story.unestimated);
                renderRow(view.storiesTotal, stats.story.total);

                renderRow(view.epicsSwag, stats.epic.swag);
                renderRow(view.epicsGrooming, stats.epic.grooming);
                renderRow(view.epicsTeam, stats.epic.team);
                renderRow(view.epicsUnestimated, stats.epic.unestimated);
                renderRow(view.epicsTotal, stats.epic.total);
            }

            function getEstimate(item) {
                var estimate = {
                    currency: "unestimated",
                    points: 0
                };

                var mostRecent = Util.findMostRecentEstimate(item);
                if (mostRecent!==undefined) {
                    estimate.currency = mostRecent.currency;
                    estimate.points = mostRecent.value;
                }

                return estimate;
            }

            function getStatus(item) {
                if (item.isComplete===true) return "finished";
                if (item.inProgress===true) return "inProgress";
                return "todo";
            }

            function processItems(stats, items, kind) {
                _.each(items, function(story) {
                    var status = getStatus(story);
                    var estimate = getEstimate(story);

                    // update the count and points for the currency.status
                    stats[kind][estimate.currency][status].count++;
                    stats[kind][estimate.currency][status].points += parseInt(estimate.points);

                    // update the count and points for the currency."total"
                    stats[kind][estimate.currency]["total"].count++;
                    stats[kind][estimate.currency]["total"].points += parseInt(estimate.points);

                    // update the count and points for the "total".status
                    stats[kind]["total"][status].count++;
                    stats[kind]["total"][status].points += parseInt(estimate.points);

                    // update the count and points for the "total"."total"
                    stats[kind]["total"]["total"].count++;
                    log("  before adding: " + stats[kind]["total"]["total"].points);
                    log('  adding: ' + estimate.points);
                    stats[kind]["total"]["total"].points += parseInt(estimate.points);
                    log("  after adding: " + stats[kind]["total"]["total"].points);
                });
            }

            function processGoals(stats, backlog) {
                _.each(backlog.items, function(item) {
                    // iunnuw???
                });
            }

            function createStats() {
                return {
                    story: {
                        swag: {
                            todo:       { count: 0, points: 0 },
                            inProgress: { count: 0, points: 0 },
                            finished:   { count: 0, points: 0 },
                            total:      { count: 0, points: 0 }
                        },
                        grooming: {
                            todo:       { count: 0, points: 0 },
                            inProgress: { count: 0, points: 0 },
                            finished:   { count: 0, points: 0 },
                            total:      { count: 0, points: 0 }
                        },
                        team: {
                            todo:       { count: 0, points: 0 },
                            inProgress: { count: 0, points: 0 },
                            finished:   { count: 0, points: 0 },
                            total:      { count: 0, points: 0 }
                        },
                        unestimated: {
                            todo:       { count: 0, points: 0 },
                            inProgress: { count: 0, points: 0 },
                            finished:   { count: 0, points: 0 },
                            total:      { count: 0, points: 0 }
                        },
                        total: {
                            todo:       { count: 0, points: 0 },
                            inProgress: { count: 0, points: 0 },
                            finished:   { count: 0, points: 0 },
                            total:      { count: 0, points: 0 }
                        }
                    },
                    epic: {
                        swag: {
                            todo:       { count: 0, points: 0 },
                            inProgress: { count: 0, points: 0 },
                            finished:   { count: 0, points: 0 },
                            total:      { count: 0, points: 0 }
                        },
                        grooming: {
                            todo:       { count: 0, points: 0 },
                            inProgress: { count: 0, points: 0 },
                            finished:   { count: 0, points: 0 },
                            total:      { count: 0, points: 0 }
                        },
                        team: {
                            todo:       { count: 0, points: 0 },
                            inProgress: { count: 0, points: 0 },
                            finished:   { count: 0, points: 0 },
                            total:      { count: 0, points: 0 }
                        },
                        unestimated: {
                            todo:       { count: 0, points: 0 },
                            inProgress: { count: 0, points: 0 },
                            finished:   { count: 0, points: 0 },
                            total:      { count: 0, points: 0 }
                        },
                        total: {
                            todo:       { count: 0, points: 0 },
                            inProgress: { count: 0, points: 0 },
                            finished:   { count: 0, points: 0 },
                            total:      { count: 0, points: 0 }
                        }
                    },
                    goal: {
                        count: 0,
                        todo:       { count: 0 },
                        finished:   { count: 0 },
                        total:      { count: 0 }
                    }
                };
            }

            function generateStats(backlog) {
                var stats = createStats();

                var itemsByKind = _.groupBy(backlog.items, 'kind');
                processItems(stats, itemsByKind['story'], 'story');
                processItems(stats, itemsByKind['epic'], 'epic');
                processGoals(stats, backlog);

                return stats;
            }

            function updateStats(backlog) {
                log("update stats....");
                var stats = generateStats(backlog);
                log("stats.story.total.total.points: " + stats.story.total.total.points);
                render(stats);
            }

            v.appendTo(container);

            return {
                update : updateStats
            }
        }

    });
