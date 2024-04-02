# com.cn.tvn.awscopy

## Configuration

For production set values in external application.properties:
https://docs.spring.io/spring-boot/docs/1.0.1.RELEASE/reference/html/boot-features-external-config.html


#### Form for XLSX file upload
* /src/main/java/com/cn/tvn/awscopy/test-forms.html

#### Swagger UI
* http://localhost:8080/swagger-ui/index.html


## Endpoints

POST
/api/v1/lists/{id}/cancel


POST
/api/v1/lists/add


GET
/api/v1/lists/{listId}/report


GET
/api/v1/lists/{id}


DELETE
/api/v1/lists/{id}


GET
/api/v1/lists/{id}/objects


GET
/api/v1/lists/status/{status}


GET
/api/v1/lists/all

object-to-copy-controller


GET
/api/v1/objects/{id}


GET
/api/v1/objects/status/{status}


GET
/api/v1/objects/status/{status}/list/{listId}

## Documentation

* http://192.168.69.12:8070/display/TVNKB/S3+-+Synchronization+Tool
* https://docs.google.com/document/d/1o4XLH4vPbkAs1WSz_4kuLW9HmGGfiah9PTEvZf1-J-g
