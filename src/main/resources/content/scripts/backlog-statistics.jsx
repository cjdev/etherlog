define(["react", "underscore", "util"], function(React, _, Util) {

    var StatisticsRow = React.createClass({
        render: function() {
            var data = this.props.data;
            return (
                <div className={"row " + this.props.cssClass}>
                    <div className="small-3 columns ">{this.props.title}</div>
                       <div className="small-2 columns text-right todo">
                            <span className="count">{data.todo.count}</span> / <span className="points">{data.todo.points}</span>
                        </div>
                        <div className="small-2 columns text-right in-progress">
                            <span className="count">{data.inProgress.count}</span> / <span className="points">{data.inProgress.points}</span>
                        </div>
                        <div className="small-2 columns text-right finished">
                            <span className="count">{data.finished.count}</span> / <span className="points">{data.finished.points}</span>
                        </div>
                        <div className="small-2 columns text-right total end">
                            <span className="count">{data.total.count}</span> / <span className="points">{data.total.points}</span>
                        </div>
                </div>);
        }
    });

    var StatisticsSection = React.createClass({
        render: function() {
            var sectionStats = this.props.data;
            var outerCssClassName = "columns small-" + this.props.gridWidth;
            var rows = [];
            {_.each(sectionStats, function(val, key){
                rows.push(<StatisticsRow key={key} cssClass={key} title={key} data={val}/>);
            })}

            return (
                <div className={outerCssClassName}>
                    <div className={"kind "+this.props.cssClass}>
                        <div className="row header">
                            <div className="small-12 columns"><h5>{this.props.sectionTitle}</h5></div>
                            <div className="row titles">
                                <div className="small-3 columns text-right empty">&nbsp;</div>
                                <div className="small-2 columns text-right todo">Todo</div>
                                <div className="small-2 columns text-right in-progress">In Progress</div>
                                <div className="small-2 columns text-right finished">Finished</div>
                                <div className="small-2 columns text-right total end">Total</div>
                            </div>
                        </div>
                        {rows}
                    </div>
                </div>);
        }
    });

    return React.createClass({
        render: function() {
            function log(msg) {
                if (false) console.log.apply(console, arguments);
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

            function generateStats(backlog) {
                var stats = createStats();

                var itemsByKind = _.groupBy(backlog.items, 'kind');
                processItems(stats, itemsByKind['story'], 'story');
                processItems(stats, itemsByKind['epic'], 'epic');
                processGoals(stats, backlog);

                return stats;
            }

            var stats = generateStats(this.props.backlog);

            return (
                <div className="stats-wrapper">
                    <StatisticsSection gridWidth="4" cssClass="stories" sectionTitle="Stories" data={stats.story}/>
                    <StatisticsSection gridWidth="4" cssClass="epics" sectionTitle="Epics" data={stats.epic}/>
                    <StatisticsSection gridWidth="4" cssClass="goals" sectionTitle="Goals" data={stats.goals}/>
                </div>);
        }
    });
});
