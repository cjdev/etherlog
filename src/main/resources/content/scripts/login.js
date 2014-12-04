define([
    "jquery",
    "authUtil",
    "foundation.reveal",
    "foundation.slider"],
    function($, authUtil) {
        
        $(document).foundation();

        console.log("isLoggedIn", authUtil.isLoggedIn());
        
        $(".button").click(function(e){
            e.preventDefault();
            var error = $(".error");
            error.fadeOut();
            
            var email = $(".email").val();
            var password = $(".password").val();
            var session = authUtil.login(email, password);
            if(session){
                var path = window.location.pathname.replace("/login", "");
                window.location = (path===""?"/":path);
            }else{
                error.fadeIn();
                setTimeout(function(){error.fadeOut();}, 3000);
            }
        });
});

