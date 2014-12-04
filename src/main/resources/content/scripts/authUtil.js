define(["jquery"], function($){

    const sessionCookieName = "session";
    
    function redirectToLoginIfNotLoggedIn(){
        if(!isLoggedIn()){
            window.location = "/login" + window.location.pathname;
        }
    }
    
    function isLoggedIn(){
        var sessionCookie = getCookie(sessionCookieName);
        var userInfo = getSessionInfo(sessionCookie);
        if(sessionCookie && userInfo){
            return true;
        }else{
            return false;
        }
    }
    
    function login(email, password){
        var result;
        
        $.ajax({
            type : "POST",
            url : "/api/sessions",
            async : false,
            data: JSON.stringify({
                email:email,
                password:password
            }),
            success : function(data, foo, xhr) {
                result = data;
                setCookie(sessionCookieName, result.id);
            }
        });
        
        
        return result;
    }
    
    function logout(){
        setCookie(sessionCookieName, "");
        window.location = "/";
    }
    
    function getCookie(c_name) {
        var c_value = document.cookie;
        var c_start = c_value.indexOf(" " + c_name + "=");
        if (c_start == -1) {
            c_start = c_value.indexOf(c_name + "=");
        }
        if (c_start == -1) {
            c_value = null;
        } else {
            c_start = c_value.indexOf("=", c_start) + 1;
            var c_end = c_value.indexOf(";", c_start);
            if (c_end == -1) { 
                c_end = c_value.length;
            }
            c_value = unescape(c_value.substring(c_start, c_end));
        }
        return c_value;
    }

    function setCookie(c_name, value, exdays) {
        var exdate = new Date();
        exdate.setDate(exdate.getDate() + exdays);
        var expirationClause = ((exdays === null) ? "" : "; expires=" + exdate.toUTCString());
        var c_value = escape(value);
        document.cookie = c_name + "=" + c_value + expirationClause + "; path=/";
    }
    function getSessionInfo(sessionId) {
        var result;
        $.ajax({
            type : "GET",
            url : "/api/sessions/" + sessionId,
            async : false,
            success : function(data) {
                result = data;
            }
        });
        return result;
    }
    
    return {
        isLoggedIn:isLoggedIn,
        login:login,
        logout:logout,
        redirectToLoginIfNotLoggedIn:redirectToLoginIfNotLoggedIn
    };
});