package demo;

import javax.inject.Named;

import static demo.Constants.FIRSTNAME;
import static demo.Constants.LASTNAME;

interface Example { // We need an instance of _this_ interface!
    String getName();
}

class Constants {
    public static final String FIRSTNAME = "firstname";
    public static final String LASTNAME = "lastname";
}

@dagger.Module
class ConfigurationMap extends java.util.TreeMap<String, String> {
    @dagger.Provides
    public ConfigurationMap getConfigurationMap() {
        return this;
    }
}

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

public class Main {
    @dagger.Component(modules = {ExampleModule.class, ConfigurationMap.class})
    interface ExampleComponent { // What do we need Dagger to build for us?
        Example example();
    }

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
