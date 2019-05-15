# Introduction to Cats Effect

This walk-through uses [Ammonite](https://ammonite.io/#Ammonite-REPL), if you want to follow along please install Ammonite and start it `amm` from the root of this project.

## Revisiting principles of Functional Programming
In programming paradigms, we have certain fundamental principles that we adhere to that greatly influence how we build software.  Most of us are accustomed to doing things in a certain way.  However, we rarely question why it is that we do those things, we never question the principles because they are beholden to just be the truth.

Fact is, everything that we do revolves around, stems from those fundamental principles.  Take some Object Oriented fundamental principles:

* Encapsulation - expose an interface, but hide the details of the implementation to users
* Polymorphism - an object can be many things at once.  This is achieved via interfaces in Java
* Inheritance - an object can inherit behavior from a base class.  We often achieve reusability and keep our code DRY via inheritance.
* Marrying state and behavior - keep an object's state hidden (encapsulation), expose behavior that works on that state
* Everything is an object - in order to follow these principles, often _everything is an object_!

Everything is an object has a profound effect on Object Oriented Programming (OOP).

**What is the first question you ask yourself when adding new behavior to an existing system?**

_Answer: _who_ owns this behavior (i.e. which object or class)._

With these fundamental principles in place for OOP, the consequences of those principles have a ripple effect on everything that you do.

* Need some new behavior?  Find a _class_ to implement it on
* How do I solve some problem?  Look to the Gang of Four design patterns, that leverage abstract classes, interfaces, and concrete classes in certain organization and interaction patterns to solve it.  Things like `AbstractFactory`, `Strategy`, `Adapter` patterns.  [Gang of Four Patterns](http://www.blackwasp.co.uk/AbstractFactory.aspx)
* Now that you have all these "classes", how do they know about each other?  This gave rise to `Inversion of Control` / `Dependency Injection`, things that are staples in OO Programming (Spring, Guice).

Much of the industry for the last 25 years has built itself on the shoulders of these "OO Principles".

## Why is Functional Programming different
FP is different than OO in that it adheres to a **different set of fundamental principles**.  These principles stemmed from Math and Logic and go back a long time.

FP principles include:
* The fundamental unit of programming is a "function"
* Functions _always_ return a value
* Functions do not have any preconceived notion of the disposition of its parameters.  If I have a function `def do(bar: String): String`, the argument `bar` is there, it has to be, and it cannot be null.
* Functions are not aware of the things that have happened before them, or the things that will happen later.
* Functions work only on their inputs, they are not aware of any outside state.
* Functions do not _modify_ state (prefer immutability).  i.e, there will be no **side effect** of running your function.

These principles also fundamentally impact the way that you program, they have a ripple effect on what you do

* Need some new behavior?  Write a function (or modify an existing function).
* How do all these functions know about each other?  Import them into scope (or pass them as arguments)
* Need to raise an error, **don't throw, return a value indicating an error condition**
* Need to represent that something maybe null, **use Option, never use null**
* Need to short circuit your program in the event of failure?  **Monads**

This gives rise to the **need** for things like `Monad`, `Functor`, and specifically libraries like `cats` and `cats-effect` that **solve problems stemming from adhering to our fundamental FP principles**.

Let's look at a simple example that adheres to our FP princples...

```scala
@ def add(a: Int, b: Int): Int = a + b
defined function add

@ add(1, 2)
res4: Int = 3

@ add(1, 2)
res5: Int = 3

@ add(-100, -1000)
res6: Int = -1100
```

We can see that `add` adheres to all of our principles:
1. It will always return a value
1. It only knows about the arguments that it is given
1. It makes no assumptions about the disposition of those arguments

## Why do we need cats-effect?
In programming (in general), we have two kinds of failure modes.  "Expected" failures (things we can reliably predict that will happen), and "Unexpected" failures (things that could possibly go wrong, typically unpredictably).

### Predictable Errors

To look at these two failure modes, let's look at writing a simple division function:

```scala
def divide(nom: Int, denom: Int): Int = nom / denom
```

**Does divide adhere to all of our principles?**

1. It does not make any assumptions of its arguments
1. It only knows about the arguments it is given
1. It does not throw, so it always returns a value (right?)

Let's look at some usage...

```scala
@ def divide(nom: Int, denom: Int): Int = nom / denom
defined function divide

@ divide(10, 5)
res8: Int = 2

@ divide(10, 0)
java.lang.ArithmeticException: / by zero
  ammonite.$sess.cmd7$.divide(cmd7.sc:1)
  ammonite.$sess.cmd9$.<init>(cmd9.sc:1)
  ammonite.$sess.cmd9$.<clinit>(cmd9.sc)
```

We can see, dividing by 0 **throws an exception**.  This *violates** our FP principle of _we always return a value_.

This is an instance of what we refer to as a "Predictable" error, i.e. one that we can 100% know under what conditions it will happen and account for it in our code.

To fix our code, we _must_ ensure that we _always_ return a value.  We indicate the failure possibility with an error, and change our response type to an `Either`...

```scala
@ case class DivideByZeroError()

@ def divideSafe(nom: Int, denom: Int): Either[DivideByZeroError, Int] = denom match {
    case 0 => Left(DivideByZeroError())
    case ok => Right(nom / denom)
  }
defined function divideSafe

@ divideSafe(10, 5)
res15: Either[DivideByZeroError, Int] = Right(2)

@ divideSafe(10, 0)
res16: Either[DivideByZeroError, Int] = Left(DivideByZeroError())
```

We have now fixed our function to adhere to our FP principles.  This has enormouse benefits as noone has to _guess_ at what may happen, the error condition is _explicit_ in the result type `Either`.

### Unpredictable Errors
One of the arguments _against_ FP is that we do not live in a **Pure** world.  We interact with databases, message queues, HTTP, and the network, which may all **fail unpredictably**.  We also "side effect" by writing to logs, saving data to a file or database.  There is no way that we can do that with FP!

Well, this is wrong.  FP does address the messiness of an impure world.  Our goal is to strive to keep our `core` code pure, and push the ugliness to the edges.

Unpredictable Errors are errors "we cannot do anything about."  Typically, these bubble out to the user and get logged loudly, forcing a retry.  We _can_ attempt to gracefully recover from the error, but that does not change the nature of the error in that we don't know when it might occur.

Let's look at getting a JDBC connection...

```scala
@ import $ivy.`mysql:mysql-connector-java:8.0.16`
import $ivy.$

@ import java.sql._
import java.sql._

@ source(DriverManager.getConnection("hi"))
/**
    251      * Attempts to establish a connection to the given database URL.
    252      * The <code>DriverManager</code> attempts to select an appropriate driver from
    253      * the set of registered JDBC drivers.
    254      *
    255      * @param url a database url of the form
    256      *  <code> jdbc:<em>subprotocol</em>:<em>subname</em></code>
    257      * @return a connection to the URL
    258      * @exception SQLException if a database access error occurs or the url is
    259      * {@code null}
    260      * @throws SQLTimeoutException  when the driver has determined that the
    261      * timeout value specified by the {@code setLoginTimeout} method
    262      * has been exceeded and has at least tried to cancel the
    263      * current database connection attempt
    264      */
    265     @CallerSensitive
    266     public static Connection getConnection(String url)
    267         throws SQLException {
    268
    269         java.util.Properties info = new java.util.Properties();
    270         return (getConnection(url, info, Reflection.getCallerClass()));
    271     }
```

(first, let's all agree that this violates every FP principle)

Looking at the **documentation**, we can infer that the failure modes for this function are **unpredictable**.  (Note: I made a point that we have to look at the documentation for details.  This is rather common in OO libraries.  In FP, we encode all the details of the function into the function signature and types we use directly)

`SQLException` can happen for many reasons, most of those unpredictable.  `SQLTimeoutException` is even worse "when some thing out there has decided this has been running too long."

In the `divide` example, we could predict the failure.  Here, we cannot, how can we possible adhere to our principles!

## What is cats-effect
**cats-effect** gives us a way to manage side-effects in our code while adhering to our FP principles.  In this section, we compare cats-effect to scala standard library `Future`, which is often used to manage side effects.

### What is a side effect?
A side effect is any result of running a function that is not implicit in it's return value.  Take the following example...

```scala
@ def log(message: String): Unit = println(message)
defined function log

@ log("hi")
hi


@ log("bye")
bye
```

The function `log` outputs the message provided to the console.  This function violates principles in that it never returns a value, but also it has an effect on the world other than working with the arguments or building the return type.

An important consideration is to imagine that this was interacting with the filesystem instead of the console.  There are many reasons this could fail (for example out of disk space or a permissions error).

The function signature `def log(message: String): Unit` implicates it as side-effecting.  Even worse, it does not encode the possibility of failure!

Also note, that this function is `strictly` or `eagerly` evaluated.  It is evaluated immediately when the function is called.

### How does Future help?
The scala standard library encodes a few possible effects:

1. This thing will take some time to complete, so it may still be running
1. This thing could fail with a `Success` or a `Failure`

We can use `Future` in order to _improve_ our function...

```scala
@ import java.time.Instant
@ import scala.concurrent.Future
@ import scala.concurrent.duration._
@ import scala.concurrent.ExecutionContext.Implicit.global

def logFuture(message: String): Future[String] = Future { Thread.sleep(1000); println("RUNNING FUTURE!!!!"); message }
```

We have definitely improved our function, as we will now _always_ return a value.  We have also encoded in the function signature that this may take some time, and identified the possibility of failure.

```scala
@ val f1 = logFuture("hi")
f1: Future[String] = Future(<not completed>)

@ RUNNING FUTURE!!!!
@

@ Await.ready(f1, 2.seconds)
res38: Future[String] = Future(Success(hi))

@ Await.ready(f1, 2.seconds)
res39: Future[String] = Future(Success(hi))
```

Doing `Await.ready` will give us the response of the future.  **The important note here is that Future is eagerly evaluated and memoizes its result!  It does not re-run every time you wait for it!!**

This is extremely important.  **Every time you run a Future you actually place an object on a Thread pool to be serviced for execution later, and create a call back that when the future is complete you populate the result of the future being evaluated.**

This behavior includes `map`, `flatMap`, `filter`, and _almost_ every method available on the `Future`!  Essentially, you are creating a string of callbacks, with each callback executing another piece of code _eagerly_ in a thread pool and memoizing its result!  ###Sheesh!###

### How does cats-effect help?
Let's repeat the example but using cats-effect.  Cats-effect introduces the `IO` monad.  Almost everything you will ever do with cats-effect will in someway use `IO`.

```scala
def logIO(message: String): IO[String] = IO { Thread.sleep(1000); println("RUNNING"); message }
defined function logIO
```

The definition of log seemingly has changed very little, we replace `Future` with `IO` and we are done!  Easy right!

By using IO, we are expressing:
1. this function will always return a value
1. evaluation of the result of the function **can fail**

We are **NOT** expressing that "this thing takes a long time and will not complete"!  **This is important!**.  `IO` is, by default, _synchronously_ evaluated.

Let's run our function...

```scala
@ val io1 = logIO("hi")
io1: IO[String] = Delay(ammonite.$sess.cmd40$$$Lambda$2437/2108838879@718bf363)

@ io1.unsafeRunSync
RUNNING
res42: String = "hi"

@ io1.unsafeRunSync
RUNNING
res43: String = "hi"
```

Notice, the return type is this `Delay`.  Also notice, we do not see our `println`.  The reason is that, unlike `Future`, `IO` is _lazily_ evaluated.  That is, it is only evaluated on demand, typically "at the end of the world" or the "edges" of your application.  By being lazy, the `value` being returned has _not yet been evaluated_.  You can think of it is a *program* that you will *eventually* execute.

####This is mind-blowing and takes some retooling your thinking machinery####

To evaluate `IO`, we have to `run` it.  The analog to `Await.result` here is `io1.unsafeRunSync`

This is vastly different than `Future`

1. `IO` is lazily evaluated, not eagerly evaluated (in the case of `Future`)
1. `IO` does not memoize its results, it actually runs it's program on every run
1. `IO` is not asynchronous
1. `IO` does not have a crazy call-back chain when using `map`, `flatMap`, etc.  Instead, you can think of it as "building a program"!

### How about errors?
`Future` is designed (like `IO`) to handle "unpredictable" errors.  It does so by returning a `Failure` in the event that something goes wrong.  Let's look at an example...

```scala
def logFutureFailed(message: String): Future[String] = Future {
  Thread.sleep(1000); println("RUNNING..."); throw new RuntimeException("future failed boss")
}

@ val f1 = logFutureFailed("hi")
f1: Future[String] = Future(<not completed>)

@
@ Await.result(f1, 2.seconds)
java.lang.RuntimeException: future failed boss
  ammonite.$sess.cmd10$.$anonfun$logFutureFailed$1(cmd10.sc:1)
  scala.concurrent.Future$.$anonfun$apply$1(Future.scala:655)
  scala.util.Success.$anonfun$map$1(Try.scala:251)
  scala.util.Success.map(Try.scala:209)
  scala.concurrent.Future.$anonfun$map$1(Future.scala:289)
  scala.concurrent.impl.Promise.liftedTree1$1(Promise.scala:29)
  scala.concurrent.impl.Promise.$anonfun$transform$1(Promise.scala:29)
  scala.concurrent.impl.CallbackRunnable.run(Promise.scala:60)
  scala.concurrent.impl.ExecutionContextImpl$AdaptedForkJoinTask.exec(ExecutionContextImpl.scala:140)

@ f1.value
res13: Option[scala.util.Try[String]] = Some(
  Failure(java.lang.RuntimeException: future failed boss)
)
```

Notice, we again see `RUNNING...` being output, because it is eagerly evaluated.  This time when we "run" our future using the `Await` syntax, the underlying exception is thrown.

The `value` is a `Failure` inside of our variable.

`IO` is very similar, except instead of a custom type for `Success` and `Failure`, it uses `Either[Throwable, A]`...

```scala
@ def logIOFailed(message: String): IO[String] = IO {
    Thread.sleep(1000)
    println("RUNNING...")
    throw new RuntimeException("io failed boss")
  }
defined function logIOFailed

@ def io1 = logIOFailed("hi")
defined function io1

@ io1.unsafeRunSync
RUNNING...
java.lang.RuntimeException: io failed boss
  ammonite.$sess.cmd14$.$anonfun$logIOFailed$1(cmd14.sc:4)
  cats.effect.internals.IORunLoop$.step(IORunLoop.scala:176)
  cats.effect.IO.unsafeRunTimed(IO.scala:321)
  cats.effect.IO.unsafeRunSync(IO.scala:240)
  ammonite.$sess.cmd16$.<init>(cmd16.sc:1)
  ammonite.$sess.cmd16$.<clinit>(cmd16.sc)
```

Very similar to `Future`.  The differences we highlighted above.

The main difference is we cannot inspect the `value` inside of the `IO`, because no value is every memoized.  If we want to see the result instead of throwing an error, we can use `attempt`...

```scala
@ io1.attempt.unsafeRunSync
RUNNING...
res17: Either[Throwable, String] = Left(java.lang.RuntimeException: io failed boss)
```

## Common conventions
**Using IO.pure**
`IO.pure` is useful for lifting a known value (not an expression to be evaluated) into a program.  Never ever use `IO.pure` unless you are 100% certain that you have a value and it will never fail; otherwise, your program may fail in unexpected ways with unexpected consequences.

**IO.unit**
Very convenient if you have something that is truly side effecting and want to always return a value

**Ignore the result of an IO**
When not using a for comprehension, you may see times when you want to "drop" the result of one IO and just return another.  You can do this using `flatMap` as an example

```scala
@ def output(message: String): IO[Unit] = IO(println(message))
@ def hello(message: String): IO[Unit] = output(message).flatMap {_ => IO(println("hello")) }
```

But, we never want to do anything with that first value, and the `.flatMap` can be annoying. Instead, let's use our fish operator..

```scala
@ def output(message: String): IO[Unit] = IO(println(message))
defined function output

@ def helloFish(message: String): IO[Unit] = output(message) *> IO(println("hello"))
defined function helloFish

@ helloFish("paul").unsafeRunSync
paul
hello
```

# Why do I want to use it?
There are a lot of reasons to use `IO` instead of `Future`.  The differences I mentioned highlight most of them.

**No implicit queueing**
If your program does a lot of `map`, `flatMap` combinations (for comprehensions), recall that every time one of those things is called we are actually creating an object that is run on a separate thread pool.  If you are handling a lot of load, this will increase memory pressure, potentially resulting in an OOM for your application.

With `IO`, you need to _explicitly_ declare concurrency, so you are in full control of how that works (another talk will cover concurrency in `IO`).

**Predictable profiling**
With `Future`, your for comprehensions `map` `flatMap` chains can be nested.  When the `onComplete` call back is actually executed is **non-deterministic**.  This makes profiling your application extremely difficult, and at times impossible.

With `IO`, you can wrap your program (or an individual `IO`) and full understand when it starts and when it stops, as there is no callback chain that the underlying machinery sorts out for you.

**Push your effects to the edges**
With `Future`, you have to think imperatively through your code, when your expression is evaluated, it is evaluated eagerly.

With `IO`, you are truly "building programs", and then execute them "at the end of the world" near the edge of your system.

**Eliminate implicit ExecutionContext**
If you have a `Future` based application, you know the pain of carrying implicit execution with you.  With `IO`, you can get rid of all of that.

Compare and contrast [src/test/scala/futures/PetService](Futures PetService) with [src/test/scala/ios/PetService](IO PetService).

**Your application is already largely synchronous!!**
If you are using for comprehensions, map, flatMap with `Future`, your program is already synchronous.  There is no need to be executing all these things on a separate thread pool.

**No more unexpected timeout errors in your tests**
If you have seen seemingly sporadic timeout errors in your tests, they can be difficult to stomp out, and usually involve increasing the timeout to a possibly ridiculous value.

With `IO`, all your tests can safely just `.unsafeRunSync`, not more unpredictable timeouts!
