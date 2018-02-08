# dagger2-named-string-inject-example

Abstract: This project demonstrates how to use `@Named("...") String`
with a runtime configuration `<String, String>` map.  This allows
Dagger to inject configuration strings determined at runtime.

_Note: This builds on https://github.com/ravn/dagger2-hello-world
which explains the minimal amount of work needed to have Dagger2
work.  This includes not being able to compile and/or run the project._

Why use `@Named("...") String`?
===

Java has traditionally favored configuration in the form of key-value
string pairs, but the problem is that communication between the place
these values are made available to the program and the places the
values are actually needed is not easily done in a transparent and
secure way.

This project demonstrates how to get Dagger to inject configuration
values like any other dependency, even when these are provided at
runtime.

The reason `@Named` is necessary is because most Java dependency
injection frameworks works on _classes_.  The class name is used to
determine what instance should be injected.  This works well for
e.g. `javax.sql.DataSource` if you just want access to the backend
database, but as all configuration strings are of type `String` an
additional qualifier is needed.  In JSR-330 this is
`javax.inject.Named` which takes a string argument which in this
scenario is the configuration _key_.  So in other words, given the key
Dagger injects the value.  The sweet spot for these are as arguments
to providers in Dagger modules!

```java
    @dagger.Provides
    Example provideExample(@Named(FIRSTNAME) String firstName, @Named(LASTNAME) String lastName) {
        return () -> "Name: " + firstName + " " + lastName;
    }
```

This provider needs two configuration strings, and as with all other
dependencies they are "just there" (seen from our point of view).


What is needed?
===

Dagger does its magic at compilation time.  Hence quite a bit of elbow
grease is generally needed to give the compiler what it needs.

The new parts needed:

* A key-value map that can be present itself to Dagger at
  runtime.
* A `@Named(X) String provideX(map)` method for each configuration key.
  
Dagger works recursively.  If a given action - like invoking a method
or calling a constructor - requires parameters, then the creation of
these are considered new actions and the process repeats.


Going through `Main.java`
===

There is a lot of moving parts here.  To make it easier to read,
everything is in `Main.java` which is less than a 100 characters wide
and less than 66 lines long so it can be printed on a single piece of
paper.

* `interface Example`
* `class Constants`
* `class ConfigurationMap`
* `class ExampleModule`
* `interface Main.ExampleComponent`
* `class Main`


`interface Example`
---

This is what our code wants to work with.  The whole reason for the
rest is to get an instance of this interface for further processing.

```
interface Example { // We need an instance of _this_ interface!
    String getName();
}
```


`class Constants`
---

```
class Constants {
    public static final String FIRSTNAME = "firstname";
    public static final String LASTNAME = "lastname";
}
```

We _could_ just use "firstname"/"lastname" instead but using a
constant gives all the usual help from the compiler and IDE's.


`class ConfigurationMap` 
---

In order for Dagger to consider a provider method, it _must_ be inside
a class annotated with `@Module`.  As of Java 8 this cannot be done
using lambda expressions, so a pragmatic simple solution is to make
the map its own module providing itself:

```java
@dagger.Module
class ConfigurationMap extends java.util.TreeMap<String, String> {
    @dagger.Provides
    public ConfigurationMap getConfigurationMap() {
        return this;
    }
}
```

`class ExampleModule`
---

Dagger can instantiate certain objects on its own, but needs help for
the rest in form of methods annotated with `@dagger.Provider` methods
placed inside a class annotated with `@dagger.Module` known to it.
The main Dagger module here is `ExampleModule`.

Each configuration key to be used in the application must have its own
provider method, and the key should be declared as a string constant.
This is quite tedious to write in the first place, but has the
following advantages (none of which are used here to save space but
would be in production code):

* Each string constant can have javadoc explaining the semantics of
  that parameter making it very easy to create documentation.  Just
  have them in the same class, and link to the generated javadoc page.
* Each provider can have its own individual "key not in map"
  handling.  As this is not common code, it tends to be very simple.
  A "fail immediately if not present" has shown to work well for our
  applications, and is what is implemented here.  Another could be to
  provide a default value. 
* The name of the provider can reflect on its purpose.  Dagger does not
  care, so there is no restrictions on this.  Typically we end up with
  `provideFirstName` for a provider for "firstname".
* Each provider can take exactly the arguments it needs.  Typically
  this is a `ConfigurationMap`, which is then resolved to the
  actual map.
* Dagger only invokes providers if needed, so only those key-value
  pairs needed, must be in the map.

So `provideExample` (which constructs the Example instance we ask
Dagger to create) takes two arguments which Dagger in turn fulfills by
invoking the appropriate providers which in turn invoke the provider
inside the `ConfigurationMap` instance passed in to the builder at run
time.  The lambda expression is just short hand for creating Example
as it is a single method interface.


```java
@dagger.Module
class ExampleModule { // What help does Dagger need?
    @dagger.Provides
    Example provideExample(@Named(FIRSTNAME) String firstName, @Named(LASTNAME) String lastName) {
        return () -> "Name: " + firstName + " " + lastName;
    }

    @dagger.Provides
    @Named(FIRSTNAME)
    String provideFirstName(ConfigurationMap map) {
        return java.util.Objects.requireNonNull(map.get(FIRSTNAME), FIRSTNAME + " not set");
    }

    @dagger.Provides
    @Named(LASTNAME)
    String provideLastName(ConfigurationMap map) {
        return java.util.Objects.requireNonNull(map.get(LASTNAME), LASTNAME + " not set");
    }
}
```

Also it is very hard to be tempted to write generic code which then in
turn ends up needing configuration.  All in all, this mean that
providers tend to be small and simple.

The providers here throw a null pointer exception with the text "X not
set" if X is not present in the configuration map.  This is probably
the easiest way to fail fast.  In a production setting a custom
exception named `ConfigurationKeyException` might be more telling.
Any other appropriate behavior could be placed here.


`interface Main.ExampleComponent`
---

A Dagger Component is the starting point for Dagger.  It lists the
methods that Dagger is to implement returning injected objectes, and
it lists the modules Dagger is to use in the `@Component` annotation.

Here we have two modules, namely the ExampleModule (which has the
provider for Example and the two configuration key-value pairs) and
ConfigurationMap (which has the provider for itself).  If any provider
fails to be found by `javac` double check that the method is annotated
with `@Provider` and the module is annotated with `@Module` as well as
listed in the `@Component` annotation of the component.

```
@dagger.Component(modules = {ExampleModule.class, ConfigurationMap.class})
interface ExampleComponent { // What do we need Dagger to build for us?
    Example example();
}
```

The Dagger compiler integration ensures that several source files are
written as part of the compilation, which implement the necessary
functionality.  All the wiring together happens during the
compilation, so the generated sources can be very fast.  The important
entry points are the sources corresponding to the components.  In this
project that is `DaggerMain_ExampleComponent`.  If there are several
components there will be several entry points.

`class Main`
---

The `main` method creates a configuration map in code, passes it to
the builder returned by the automatically generated
`DaggerMain_ExampleComponent.builder()` to complete its configuration
so `build()` can return a fully functional `ExampleComponent`.


```java
public class Main {
// ...
    public static void main(String[] args) {
        ConfigurationMap map = new ConfigurationMap();
        map.put("firstname", "Edward");
        map.put("lastname", "Snowden");
        // If compilation fails, see README.md
        ExampleComponent daggerGeneratedComponent =
                DaggerMain_ExampleComponent.builder().configurationMap(map).build();

        Example example = daggerGeneratedComponent.example();
        System.out.println(example.getName());
    }
}
```

It is now simple to invoke the `example()` method in the
`ExampleComponent` instance given to use by Dagger, to have an
`Example` object where getName() returns "Edward Snowden".

The configuration map could come from any source.  We have found that
for command line applications (where we use Dagger) a combination of
having a property filename with additional "key=value" pairs on the
command line works well.  Depending on the application it might also
be relevant to load some values from system properties and environment
variables.

Dagger is unfortunately not well suited for web applications.  We have
done some work with using context init parameters instead in Tomcat
and Jetty in addition to CDI in TomEE and Glassfish which works
relatively well.


Conclusion
===

This project demonstrates that a Dagger command line application can
have a key-value configuration map provided at runtime.









