## No connection establish, only use https request/response

### Request
* API List Request
    * GET /{Project}  

* API STATUS Request
  - GET /{Project}/{Command}  
    
* API RUN Request
  - POST /{Project}/{Command}   
    with Body { request parameters }
    
### Response    
* API List Request
  - Success : 200 with response body
  - No such Project or Command : 404  
      
* API STATUS Request
  - Success : 200 with response body
  - No such Project or Command : 404

* API RUN Request
    - Success : 200 with response body   
    - Invalid parameter : 400   
    - No such Project or Command : 404   
    - TIMEOUT : 408
