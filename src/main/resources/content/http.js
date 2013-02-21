/*jshint newcap: false*/
/*global $, HttpClient:true */
function HttpClient(request, opts) {
    var defaults = { ajax: $.ajax, async: "true" },
        options = $.extend(defaults, opts);

    function parseHeaders(headerString) {
        var headers = {},
            headerStrings = headerString.split("\n");

        $.each(headerStrings, function (index, header) {
            if (header !== "") {
                var headerTuple = header.split(": ");
                headers[headerTuple[0]] = headerTuple[1];
            }
        });

        return headers;
    }

    options.ajax({
            type: request.method,
            url: request.url,
            processData: false,
            data: request.data,
            dataType: "text",
            headers: request.headers,
            async: options.async,
            cache: options.cache,
            success: function (data, status, xhr) {
                var response = {
                    status: xhr.status,
                    body: xhr.responseText,
                    headers: parseHeaders(xhr.getAllResponseHeaders())
                };
                return request.onResponse(response);
            },
            error: function (xhr, status, err) {
                var response = {
                    status: xhr.status,
                    body: xhr.responseText,
                    headers: parseHeaders(xhr.getAllResponseHeaders())
                };
                return request.onResponse(response);
            }
        });
}
