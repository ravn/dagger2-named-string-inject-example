package demo;

import javax.inject.Named;
import java.util.Objects;
import java.util.TreeMap;

import static demo.Constants.FIRSTNAME;
import static demo.Constants.LASTNAME;

interface ExampleInterface { // We need an instance of _this_ interface!
    String getName();
}

class Constants {
    public static final String FIRSTNAME = "firstname";
    public static final String LASTNAME = "lastname";
}

@dagger.Module
class ConfigurationMap extends TreeMap<String, String> {
    @dagger.Provides
    public ConfigurationMap getConfigurationMap() {
        return this;
    }
}

// What help does Dagger need?
@dagger.Module
class ExampleModule {

    @dagger.Provides
    ExampleInterface provideExampleInterface(@Named(FIRSTNAME) String firstName, @Named(LASTNAME) String lastName) {
        return () -> "Name: " + firstName + " " + lastName;
    }

    @dagger.Provides
    @Named(FIRSTNAME)
    String provideFirstName(ConfigurationMap map) {
        return Objects.requireNonNull(map.get(FIRSTNAME), FIRSTNAME + " not set");
    }

    @dagger.Provides
    @Named(LASTNAME)
    String provideLastName(ConfigurationMap map) {
        return Objects.requireNonNull(map.get(LASTNAME), LASTNAME + " not set");
    }
}

public class Main {

    // What do we need Dagger to build and what information should Dagger use?
    @dagger.Component(modules = {ExampleModule.class, ConfigurationMap.class})
    interface ExampleComponent {
        ExampleInterface example();
    }

    public static void main(String[] args) throws Exception {
        ConfigurationMap map = new ConfigurationMap();
        map.put("firstname", "Edward");
        map.put("lastname", "Snowden");
        // If compilation fails, see README.md
        ExampleComponent daggerGeneratedComponent = DaggerMain_ExampleComponent.builder().configurationMap(map).build();

        ExampleInterface example = daggerGeneratedComponent.example();
        System.out.println(example.getName());
    }
}
