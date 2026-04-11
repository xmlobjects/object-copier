![build](https://img.shields.io/github/actions/workflow/status/xmlobjects/object-copier/object-copier-build.yml?logo=Gradle)
![release](https://img.shields.io/github/v/release/xmlobjects/object-copier?display_name=tag)
![maven](https://img.shields.io/maven-central/v/org.xmlobjects/object-copier)
[![license](https://img.shields.io/badge/license-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

# object-copier

A flexible deep and shallow copy framework for Java objects.

## Table of Contents

- [Latest release](#latest-release)
- [Features](#features)
- [Maven artifact](#maven-artifact)
- [Quick Start](#quick-start)
- [Default Copy Behaviour](#default-copy-behaviour)
- [Copy Modes](#copy-modes)
- [Copy Sessions](#copy-sessions)
- [Configuration](#configuration)
- [Built-in Identity Types](#built-in-identity-types)
- [Custom Cloners](#custom-cloners)
- [Copyable Interface](#copyable-interface)
- [@CopyCreator](#copycreator)
- [@CopyIgnore](#copyignore)
- [CopyCallback](#copycallback)
- [Module System](#module-system)
- [Building](#building)
- [Contributing](#contributing)
- [License](#license)

## Latest release
The latest stable release of object-copier is 1.2.0.

Download the latest object-copier release binaries [here](https://github.com/xmlobjects/object-copier/releases/latest).
Previous releases are available from the [releases section](https://github.com/xmlobjects/object-copier/releases).

## Features

- **Deep and shallow copy** of arbitrary Java objects
- **Reflection-based by default** – copies object fields via reflection
- **Circular reference detection** via reusable copy sessions
- **Pluggable cloners** – register custom `TypeCloner` implementations per type
- **Superclass cloner inheritance** – a cloner registered for `Animal` is automatically used for `Dog`
- **`Copyable` interface** – let objects control their own copy behaviour
- **`CopyCallback` interface** – `preCopy` / `postCopy` lifecycle hooks
- **`@CopyCreator`** – custom factory method for instance creation
- **`@CopyIgnore`** – exclude individual fields from copying
- **Built-in support** for collections, maps, arrays, `Optional`, enums, records and all common JDK value types
- **Thread-safe** after construction

## Maven artifact
object-copier is available as Maven artifact from the
[Maven Central Repository](https://search.maven.org/artifact/org.xmlobjects/object-copier). To add object-copier to your
project with Maven, add the following code to your `pom.xml`. You may need to adapt the object-copier version number.

**Maven**
```xml
<dependency>
    <groupId>org.xmlobjects</groupId>
    <artifactId>object-copier</artifactId>
    <version>1.2.0</version>
</dependency>
```

**Gradle**
```groovy
implementation 'org.xmlobjects:object-copier:1.2.0'
```

## Quick Start

```java
// Create a default Copier (no configuration needed)
Copier copier = CopierBuilder.newCopier();

// Deep copy
MyObject clone = copier.deepCopy(original);

// Shallow copy
MyObject shallowClone = copier.shallowCopy(original);
```

A `Copier` is immutable after construction and safe to share across threads.

The examples above use the default configuration described next.

## Default Copy Behaviour

By default, object-copier performs copying via reflection. This includes creating instances and reading/writing fields of a class.

Important defaults:

- `final` fields are skipped in general and are not copied
- `static` and synthetic fields are skipped
- A no-arg constructor is expected for default instance creation

If a class does not meet these requirements, use one of the supported alternatives:

- Implement `Copyable` to control copy behaviour in the type itself
- Use `@CopyCreator` to provide custom instance creation
- Register a custom `TypeCloner` (or `ObjectCloner`) via `CopierBuilder`

## Copy Modes

| Mode | Behaviour |
|------|-----------|
| `deepCopy` | All reachable objects are recursively cloned |
| `shallowCopy` | Only the top-level object is cloned; field references are shared |

Both modes support an optional `dest` target object and an optional `template` class:

```java
// Copy into an existing object
copier.deepCopy(src, dest);

// Copy using a superclass as the field template
copier.deepCopy(src, dest, AbstractBase.class);
```

## Copy Sessions

A `CopySession` tracks all clones created during a copy operation and is used to detect circular references. By default each top-level copy call creates its own session. Pass an explicit session to share the clone cache across multiple calls:

```java
try (CopySession session = copier.createSession()) {
    BuildingA cloneA = copier.deepCopy(buildingA, session);
    BuildingB cloneB = copier.deepCopy(buildingB, session);
    // Cross-references between A and B are resolved correctly
}
```

`CopySession` implements `AutoCloseable`. Closing it releases the internal clone map.

`CopySession` is not thread-safe and must not be shared across concurrent threads.
Use one `CopySession` per thread when copying concurrently.

You can also look up a previously created clone:

```java
MyObject clone = session.lookupClone(original, MyObject.class);
```

## Configuration

Use `CopierBuilder` to configure a `Copier`:

```java
Copier copier = CopierBuilder.newInstance()
    .withCloner(MyType.class, new MyTypeCloner())
    .withSelfCopy(ImmutablePoint.class)   // returned as-is, no copy
    .withNullCopy(Placeholder.class)      // always returns null
    .build();
```

### `withSelfCopy`

Types registered with `withSelfCopy` are returned as-is without cloning. Useful for truly immutable types that are not already detected automatically (e.g. custom value objects).

### `withNullCopy`

Types registered with `withNullCopy` are replaced with `null` during a copy. Useful for excluding specific types such as caches or external handles.

## Built-in Identity Types

The following JDK types are automatically treated as immutable and returned as-is (no copy):

- All primitive wrappers (`Integer`, `Long`, `Boolean`, …)
- `String`, `BigDecimal`, `BigInteger`
- `URI`, `URL`, `UUID`, `Pattern`, `Charset`, `Locale`, `Currency`
- All `java.time` types (`LocalDate`, `ZonedDateTime`, `Duration`, …)
- `Enum` constants and `record` types
- `Collections.emptyList()`, `emptyMap()`, `emptySet()`
- `OptionalInt`, `OptionalLong`, `OptionalDouble`

## Custom Cloners

Extend `TypeCloner<T>` to control instantiation and field copying for a specific type:

```java
public class PersonCloner extends TypeCloner<Person> {

    @Override
    protected Person newInstance(Person src, CopyMode mode, CopyContext context) {
        return new Person(src.getId()); // custom construction
    }

    @Override
    protected void deepCopy(Person src, Person dest, CopyContext context) {
        dest.setName(context.deepCopy(src.getName()));
        dest.setAddress(context.deepCopy(src.getAddress()));
    }
}

Copier copier = CopierBuilder.newInstance()
    .withCloner(Person.class, new PersonCloner())
    .build();
```

Extend `ObjectCloner<T>` if you only need custom instantiation but want automatic reflection-based field copying:

```java
public class PersonCloner extends ObjectCloner<Person> {

    public PersonCloner() {
        super(Person.class);
    }

    @Override
    protected Person newInstance(Person src, CopyMode mode, CopyContext context) {
        return new Person(src.getId());
    }
}
```

### Superclass Cloner Inheritance

A cloner registered for a superclass is automatically used for all subclasses that have no cloner of their own:

```java
CopierBuilder.newInstance()
    .withCloner(AbstractFeature.class, new FeatureCloner())
    .build();
// FeatureCloner is also used for Building, Road, etc.
```

## `Copyable` Interface

Implement `Copyable<T>` to let a class control its own copy behaviour. This is the recommended approach for complex class hierarchies:

```java
public class Building extends AbstractFeature implements Copyable<Building> {

    @Override
    public void deepCopyTo(Building dest, CopyContext context) {
        dest.setName(context.deepCopy(getName()));
        dest.setParts(context.deepCopy(getParts()));
    }
}
```

The default implementations of `newInstance`, `shallowCopyTo` and `deepCopyTo` fall back to reflection, so you only need to override what differs.

## `@CopyCreator`

Use `@CopyCreator` on a `newInstance(CopyMode, CopyContext)` method when a class cannot be instantiated via a no-arg constructor.
In contrast to the `Copyable` interface, the `@CopyCreator` method can be private:

```java
public class ImmutableId {

    private String value;

    private ImmutableId(String value) {
        this.value = value;
    }

    @CopyCreator
    private ImmutableId newInstance(CopyMode mode, CopyContext context) {
        return new ImmutableId(value);
    }
}
```

## `@CopyIgnore`

Annotate fields that should be excluded from all copy operations:

```java
public class MyObject {
    private String name;

    @CopyIgnore
    private transient CachedResult cache; // never copied
}
```

`@CopyIgnore` is evaluated once per class at first copy and then cached permanently – no runtime overhead.

## `CopyCallback`

Implement `CopyCallback` to receive lifecycle notifications during copying. Both `src` and `clone` receive the callbacks independently:

```java
public class Building implements Copyable<Building>, CopyCallback {

    @Override
    public void preCopy(CopyMode mode, CopyContext context) {
        // called on src before the clone is created
    }

    @Override
    public void postCopy(CopyMode mode, CopyContext context) {
        // called on clone after all fields have been copied
    }
}
```

`isRoot` is `true` only for the top-level object of a copy operation.

### Excluding objects during copy

`CopyContext` provides `exclude(Object)` and `include(Object)` to temporarily prevent an object from being copied
during the current session. An excluded object is returned as-is without being registered in the session's clone map,
keeping it consistent for subsequent copy operations in the same session.

This is particularly useful when an object holds a reference to a parent that should not be recursively copied:

```java
public interface Child extends CopyCallback {
    Child getParent();
    void setParent(Child parent);

    @Override
    default void preCopy(CopyMode mode, CopyContext context) {
        if (context.isRoot()) {
            context.exclude(getParent());  // parent returned as-is, not copied
        }
    }

    @Override
    default void postCopy(CopyMode mode, CopyContext context) {
        if (context.isRoot()) {
            context.include(getParent());  // re-enable normal copying for parent
            setParent(null);               // clone is detached from the original parent
        }
    }
}
```

Note that `exclude` differs from `withSelfCopy` on `CopyContext`: `exclude` is temporary and
does not register the excluded object in the clone map, while `withSelfCopy` permanently registers
it for the lifetime of the session.

## Module System

The framework is compatible with the Java module system. Classes in named modules must open their packages to allow reflection-based field access:

```java
// module-info.java of the module containing classes to be copied
module com.example.myapp {
    opens com.example.myapp.model to org.xmlobjects.copy; // adjust to actual module name
}
```

Alternatively, implement `Copyable` or register a `TypeCloner` – both bypass reflection entirely.

## Building
object-copier requires Java 17 or higher. The project uses [Gradle](https://gradle.org/) as build system. To build the
library from source, clone the repository to your local machine and run the following command from the root of the
repository.

    > gradlew installDist

The script automatically downloads all required dependencies for building the module. So make sure you are connected
to the internet.

The build process creates the output files in the folder `build/install/object-copier`. Simply put the
`object-copier-<version>.jar` library file and its mandatory dependencies from the `lib` folder on your modulepath to
start developing with object-copier. Have fun :-)

## Contributing
* To file bugs found in the software create a GitHub issue.
* To contribute code for fixing reported issues create a pull request with the issue id.
* To propose a new feature create a GitHub issue and open a discussion.

## License
object-copier is licensed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).
See the `LICENSE` file for more details.
