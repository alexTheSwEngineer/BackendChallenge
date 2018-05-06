# N26 Challenge


## Build & Run

```
 git clone git@github.com:alexTheSwEngineer/BackendChallenge.git
 cd BackendChallenge
 mvn clean install
 mvn eclipse:clean eclipse:eclipse
 mvn idea:clean idea:idea
 java -jar target/backendchallenge-0.0.1-SNAPSHOT.jar
 
```

The solution is a spring boot application that runs on port 8083, if that is busy you can change it by:

```
java -jar target/backendchallenge-0.0.1-SNAPSHOT.jar --server.port=8082 #or any other port

```
The mvn clean install command runs some tests that expect exceptions so don't worry about the stack traces in the log.

## Try it out

You can test the system it by hitting the specified endpoints but that is cumbersome. I have provided some debug GET endpoints to make things easier and a **[postman collection](https://www.getpostman.com/collections/0d0997bec4c427d7abe4)** with everything:
1) GET **/debug/time** returns the timestamp of utc now
2) GET **/debug/createrandom**  creates a random transaction that happened in the last 60 seconds and pushes it in the transaction service. It then gets the statistics. It returns all of this information in jason format
3) POST **/api/transactions**
4) GET **/api/statistics/latest**

Another usefull method is to run
```
 watch -n 0,2 curl http://localhost:8083/api/statistics/latest

```
while hitting the POST endpoint
Finaly, you can look at the logs which are minimal but pain some kind of picture of what is happening

## Tests
Tests are run via: 
```
mvn test 

```
The coverage can be seen via the cobertura plugin, it stores them in target/site/cobertura/index.html:
```
mvn cobertura:cobertura 
google-chrome google-chrome target/site/cobertura/index.html

```


## Consideration 
A big assumption is that the system handles a lot of transactions. Lack of transactions will make the statistics stale since they only get updated when a transaction occurs.