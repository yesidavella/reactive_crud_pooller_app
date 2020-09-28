# KRY code assignment

One of our developers built a simple service poller.
The service consists of a backend service written in Vert.x (https://vertx.io/) that keeps a list of services (defined by a URL), and periodically does a HTTP GET to each and saves the response ("OK" or "FAIL").

Unfortunately, the original developer din't finish the job, and it's now up to you to complete the thing.
Some of the issues are critical, and absolutely need to be fixed for this assignment to be considered complete.
There is also a wishlist of features in two separate tracks - if you have time left, please choose *one* of the tracks and complete as many of those issues as you can.

Critical issues (required to complete the assignment):

- Whenever the server is restarted, any added services disappear
- There's no way to delete individual services
- We want to be able to name services and remember when they were added
- The HTTP poller is not implemented

Frontend/Web track:
- We want full create/update/delete functionality for services
- The results from the poller are not automatically shown to the user (you have to reload the page to see results)
- We want to have informative and nice looking animations on add/remove services

Backend track
- Simultaneous writes sometimes causes strange behavior
- Protect the poller from misbehaving services (for example answering really slowly)
- Service URL's are not validated in any way ("sdgf" is probably not a valid service)
- A user (with a different cookie/local storage) should not see the services added by another user

Spend maximum four hours working on this assignment - make sure to finish the issues you start.

Put the code in a git repo on GitHub and send us the link (niklas.holmqvist@kry.se) when you are done.

Good luck!

# Building
We recommend using IntelliJ as it's what we use day to day at the KRY office.
In intelliJ, choose
```
New -> New from existing sources -> Import project from external model -> Gradle -> select "use gradle wrapper configuration"
```

You can also run gradle directly from the command line:
```
./gradlew clean run
```

# Usage

This app has 4 basic operations related to services:
- List Services: GET localhost:8080/service
- Record one Service: POST localhost:8080/service
- Delete service by name: localhost:8080/service/{serviceName}
- Update Service name: PATCH localhost:8080/service/{currentName}/{newName}

## Inserting services
To insert a service you just need to use the POST method with the next JSON template, replacing for the values you deserve:
```json
{
"name":"elGuardian",
"url":"theguardian.com"
}
```
Be aware that the URL is protected by REGEX, so prefixes like HTTP, https or www are not valid.
VALID URL|INVALID URL
------------- | -------------
eltiempo.com  | www.eltiempo.com
eltiempo.com/mundo  | https://www.eltiempo.com/mundo 

**NOTE:** Now, if you user the GET method you will see all the services your web client has inserted.

## Cookie implementation

Whenever you use the app, the client(postman or web browser) will store a cookie called **remoteLiviClient** which will have a long random value created by the Java function UUID.randomUUID().
So, when you call the rest service GET {host}/service, you will get as JSON **ONLY** the services inserted using "POST" by the same web client (e.g. postman).

## EXECUTION FROM COMMAND LINE

Download the project, navigate to the root project folder called \*reactive_crud_pooller_app-master\* and execute the next commands:
```bash
./gradlew clean build
./gradlew run
```
**NOTE:** You will see in the CLI a kind of traces showing you what actions are taking place by the poller, regarding the services' URLs states.
