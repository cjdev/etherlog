define([
    "jquery",
    "util",
    "uuid"
],
    function ($, Util, uuid) {

        return function (item, container, onChangeCB, onDone) {

            var v = $(
                    '        <div class="small-6 columns">' +
                    '            <div class="row collapse">' +
                    '                <div class="small-4 columns">' +
                    '                    <input class="estimate-value" type="text" placeholder="estimate">' +
                    '                </div>' +
                    '                <div class="small-6 columns">' +
                    '                    <select class="estimate-currency">' +
                    '                        <option></option>' +
                    '                        <option>swag</option>' +
                    '                        <option>grooming</option>' +
                    '                        <option>team</option>' +
                    '                    </select>' +
                    '                </div>' +
                    '                <div class="small-2 columns postfix-radius">' +
                    '                    <a href="#" class="button tiny postfix done-button">Done</a>' +
                    '                </div>' +
                    '            </div>' +
                    '        </div>'
            );

            var view = {
                currencies : v.find(".estimate-currency"),
                value : v.find('.estimate-value'),
                doneButton: v.find(".done-button")
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
                        estimate.when = Util.getTime();

                        onChangeCB();
                    }
                }
            }

            view.doneButton.click(onDone);

            view.currencies.bind("change", onChange);
            v.bind("keypress keydown keyup change", onChange);
            v.appendTo(container);
        }
    });
