package com.datacentric.timesense.utils.storage;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class StorageProviderConfig {

    @Value("${com.datacentric.timesense.utils.storage.IStorageProvider.impl}")
    private String implClassName;

    private final Environment environment;

    public StorageProviderConfig(Environment environment) {
        this.environment = environment;
    }

    @Bean
    public IStorageProvider storageProvider() throws Exception {
        if (implClassName == null || implClassName.isEmpty()) {
            throw new IllegalArgumentException(
                "Property IStorageProvider.impl not set in application.properties");
        }

        Class<?> clazz = Class.forName(implClassName);

        if (!IStorageProvider.class.isAssignableFrom(clazz)) {
            throw new IllegalArgumentException(implClassName +
                " does not implement IStorageProvider");
        }

        return instantiateProvider(clazz);
    }

    /**
     * Looks for the appropriate constructor to instantiate the provider.
     *
     * The priority is:
     *   1. Constructor with parameters annotated with @StorageProperty
     *   2. No-argument constructor
     *
     * @param clazz The class to instantiate
     * @return An instance of IStorageProvider
     */
    private IStorageProvider instantiateProvider(Class<?> clazz) throws Exception {
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();

        for (Constructor<?> constructor : constructors) {
            Parameter[] parameters = constructor.getParameters();

            if (parameters.length == 0 || !allArgumentsStorageProperty(parameters)) {
                continue;
            }

            // Compute the constructor arguments values
            Object[] args = new Object[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                StorageProperty annotation = parameters[i].getAnnotation(StorageProperty.class);
                String propertyName = annotation.value();
                String defaultValue = annotation.defaultValue();

                String value = environment.getProperty(propertyName);
                if (value == null) {
                    if (defaultValue.isEmpty()) {
                        throw new IllegalArgumentException(
                            "Required property '" + propertyName +
                            "' not found for " + clazz.getSimpleName());
                    }
                    value = defaultValue;
                }

                args[i] = value;
            }

            return (IStorageProvider) constructor.newInstance(args);
        }

        // If we didn't find a constructor with @StorageProperty, try no-arg constructor
        try {
            Constructor<?> noArgConstructor = clazz.getDeclaredConstructor();
            return (IStorageProvider) noArgConstructor.newInstance();
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(
                clazz.getSimpleName() + " must have either a no-arg constructor or " +
                "a constructor with @StorageProperty annotated parameters", e);
        }
    }

    private boolean allArgumentsStorageProperty(Parameter[] parameters) {
        for (Parameter param : parameters) {
            if (!param.isAnnotationPresent(StorageProperty.class)) {
                return false;
            }
        }
        return true;
    }
}
