     
  function queryParams(){
      const query = window.location.search;
      var params = {};
      
      if(query){
          const pairs = query.replace(/\?/, '').split("&");
      $.each(pairs, function(i, pair){
          var keyValue = pair.split("=");
              params[keyValue[0]] = keyValue[1];
          });
      }
      
      return params;
  }