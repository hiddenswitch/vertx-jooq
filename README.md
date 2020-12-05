# vertx-jooq
A [jOOQ](http://www.jooq.org/)-CodeGenerator to create [vertx](http://vertx.io/)-ified DAOs and POJOs!
Perform all CRUD-operations asynchronously and convert your POJOs from/into a `io.vertx.core.json.JsonObject` using the API and
driver of your choice.

## release 6.0.0
The following dependencies have been upgraded:
- upgrade junit to 4.13.1
- upgrade vertx to 3.9.4
- upgrade jooq to 3.13.5
The following bug has been fixed
- fix an issue for windows users and the generated package names #159 

**Note**: this will most likely be the last release depending on the vertx 3.x and jooq 3.13.x branches. As vertx now integrates first-class `CompletionStage` support and the `io.vertx.core.Future`-API has improved dramatically, we will most likely drop the `-completablefuture` modules entirely in vertx-jooq 6.  

## different needs, different apis
![What do you want](https://media.giphy.com/media/E87jjnSCANThe/giphy.gif)

Before you start generating code using vertx-jooq, you have to answer these questions:
- What API do you want to use? There are three options:
  - a `io.vertx.core.Future`-based API. This is `vertx-jooq-classic`.
  - a [rxjava2](https://github.com/ReactiveX/RxJava) based API. This is `vertx-jooq-rx`.
  - an API that returns a [vertx-ified implementation](https://github.com/cescoffier/vertx-completable-future)
  of `java.util.concurrent.CompletableFuture` for all async DAO operations. This is `vertx-jooq-completablefuture`.
- How do you want to communicate with the database? There are two options:
  - Using good old JDBC, check for the modules with `-jdbc` suffix.
  - Using this [asynchronous](https://github.com/jasync-sql/jasync-sql) database driver, check for `-async` modules.
  - Using this [reactive](https://github.com/reactiverse/reactive-pg-client) postgres database driver, check for `-reactive` modules.
- Advanced configuration:
  - Support for [Guice](https://github.com/google/guice) dependency injection
  - Generation of `io.vertx.codegen.annotations.@DataObject`-annotations for your POJOs
  

When you made your choice, you can start to configure the code-generator. This can be either done programmatically or
 using a maven- / gradle-plugin (recommended way). Please check the documentation in the module of the API of your choice how to set it up:

- [`vertx-jooq-classic-jdbc`](vertx-jooq-classic-jdbc)
- [`vertx-jooq-classic-reactive`](vertx-jooq-classic-reactive)
- [`vertx-jooq-rx-jdbc`](vertx-jooq-rx-jdbc)
- [`vertx-jooq-rx-reactive`](vertx-jooq-rx-reactive)
- [`vertx-jooq-completablefuture-jdbc`](vertx-jooq-completablefuture-jdbc)
- [`vertx-jooq-completablefuture-reactive`](vertx-jooq-completablefuture-reactive)


## example
Once the generator is set up, it will create DAOs like in the code snippet below (classic-API, JDBC, no dependency injection):
```java
//Setup your jOOQ configuration
Configuration configuration = ...

//setup Vertx
Vertx vertx = Vertx.vertx();

//instantiate a DAO (which is generated for you)
SomethingDao dao = new SomethingDao(configuration,vertx);

//fetch something with ID 123...
dao.findOneById(123)
    .onComplete(res->{
    		if(res.succeeded()){
        		vertx.eventBus().send("sendSomething", res.result().toJson())
    		}else{
    				System.err.println("Something failed badly: "+res.cause().getMessage());
    		}
    });

//maybe consume it in another verticle
vertx.eventBus().<JsonObject>consumer("sendSomething", jsonEvent->{
    JsonObject message = jsonEvent.body();
    //Convert it back into a POJO...
    Something something = new Something(message);
    //... change some values
    something.setSomeregularnumber(456);
    //... and update it into the DB
    Future<Integer> updatedFuture = dao.update(something);
});

//or do you prefer writing your own type-safe SQL? Use the QueryExecutor from the DAO...
ClassicQueryExecutor queryExecutor = dao.queryExecutor();
//... or create a new one when there is no DAO around :)
queryExecutor = new JDBCClassicGenericQueryExecutor(configuration,vertx);
Future<Integer> updatedCustom = queryExecutor.execute(dslContext ->
				dslContext
				.update(Tables.SOMETHING)
				.set(Tables.SOMETHING.SOMEREGULARNUMBER,456)
				.where(Tables.SOMETHING.SOMEID.eq(something.getSomeid()))
				.execute()
);

//check for completion
updatedCustom.onComplete(res->{
		if(res.succeeded()){
				System.out.println("Rows updated: "+res.result());
		}else{
				System.err.println("Something failed badly: "+res.cause().getMessage());
		}
});
```

# handling custom datatypes
The generator will omit datatypes that it does not know, e.g. `java.sql.Timestamp`. To fix this, you can subclass the generator, handle these types and generate the code using your generator.
 See the `handleCustomTypeFromJson` and `handleCustomTypeToJson` methods in the `AbstractVertxGenerator` or checkout the [`CustomVertxGenerator`](vertx-jooq-generate/src/test/java/io/github/jklingsporn/vertx/jooq/generate/custom)
 from the tests.
 
# disclaimer
This library comes without any warranty - just take it or leave it. Also, the author is neither connected to the
company behind vertx nor the one behind jOOQ.

# How to run tests

> I receive a "Too many open files" exception on **macOS**

Increase your file limits:

```
sudo sysctl -w kern.maxfiles=5242880
sudo sysctl -w kern.maxfilesperproc=524288
ulimit -n 200000
sudo launchctl limit maxfiles 524288 5242880
```