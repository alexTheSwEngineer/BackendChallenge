# N26 Challenge


## Build & Run

```
#$ mvn clean install
#$ mvn eclipse:clean eclipse:eclipse
#$ mvn idea:clean idea:idea
#$ java -jar 


```

Main ideas/architecture:
High lvl:
- GET endpoint should just return a precalculated result. Either from heap or with a o(1) db read (if that is even possible in theory)
- POST endpoint has to triger aggregation
- Concerns: 
	1) Is a query to db with "limit 1" a O(1) operration? 
	2) On the other hand is reading from a static field a scalable solution (horisontal scaling is more complex)?
	3) We need some decoupling of concerns between the insert and reagregate operation even though one should triger the other? 
	4) Concurency: what happenes if one insert transaction operation ends, but before it can update the statistics another, newer insert transaction operation finishes both the insert and the update statistics part? We need to prevent overwriting with stale state due to concurency issues.
	5) What happenes if there are no transactions for a long time? Need for scheduled update...ish.
	4) I hate dates in programing, good thing it is in utc.

- Proposed solution:
 A system wehere the getstatistics is just returning a precalculated field with aggregate results and insert a transaction is an expensive operation (aparently both time and moneyvise) because it
updates the statistics. It does so by firing an event each time. 
The existance of a (very very very) **rudimentary event system** is important because: 
	a) decouple insert and update statistics (we dont want to manually triger the code to update the statistics in every new endpoint we create. What if we end up needing statistics for 30 seconds too? We find every existing endpoint and add one more call to a diferent aggregator?)
	b) Abstract away the transactionality and implementation of the update part. Do we want a transaction to fail if the code for updating the statistics fails? Or is it fire and forget? Is it via method call or via rest call?

The **statistics module** should know the curent 60 secs state in o(1) time. It should also know how to update itself, even it gets a "confused" order of events (good way to do this is to ignore events with event time smaller then the last update time. Ofcourse this is not a dayetime but some sybolik forward only time like for example transaction id in the case of autoincremented ids).

Tests:
-Avoid as much concurency issues as possible  by using immutable objects wherever possible and the syncronized keyword wherever unavoidable
-Avoid instantiation of date/time  objects in favour of date services that can be mocked.(Avoid all static access in favour of injectables for that matter)
