package demo;

import javax.inject.Named;
import java.util.Objects;
import java.util.TreeMap;

interface ExampleInterface { // Our work unit.
    String getName();
}

class Constants {
    public static String FIRSTNAME = "firstname";
    public static String LASTNAME = "lastname";
}

class ConfigurationMap extends TreeMap<String, String> {
}

@dagger.Module
        // What help does Dagger need?
class ExampleModule {
    @dagger.Provides
    ExampleInterface provideExampleInterface(
            @Named("firstname") String firstName,
            @Named("lastname") String lastName
    ) {
        return () -> "Name: " + firstName + " " + lastName;
    }

    @dagger.Provides
    @Named("firstname")
    String provideFirstName(ConfigurationMap map) {
        return Objects.requireNonNull(map.get("firstname"), "firstname" + " not set");
    }

    @dagger.Provides
    @Named("lastname")
    String provideLastName(ConfigurationMap map) {
        return Objects.requireNonNull(map.get("lastname"), "lastname" + " not set");
    }

    @dagger.Provides
    public ConfigurationMap getConfigurationMap() {
        ConfigurationMap map = new ConfigurationMap();
        map.put("firstname", "Edward");
        map.put("lastname", "Snowden");
        return map;
    }

}

public class Main {
    // What do we need Dagger to build?
    @dagger.Component(modules = ExampleModule.class)
    interface ExampleComponent {
        ExampleInterface example();
    }

    public static void main(String[] args) throws Exception {
        //ConfigurationMap map = new ConfigurationMap();
        //map.put("firstname", "Edward");
        //map.put("lastname", "Snowden");
        // If compilation fails, see README.md
        ExampleComponent daggerGeneratedComponent = DaggerMain_ExampleComponent.builder().build();

        ExampleInterface example = daggerGeneratedComponent.example();
        System.out.println(example.getName());
    }
}
