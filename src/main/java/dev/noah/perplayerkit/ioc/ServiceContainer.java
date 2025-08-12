/*
 * Copyright 2022-2025 Noah Ross
 *
 * This file is part of PerPlayerKit.
 *
 * PerPlayerKit is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * PerPlayerKit is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for
 * more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with PerPlayerKit. If not, see <https://www.gnu.org/licenses/>.
 */
package dev.noah.perplayerkit.ioc;

import dev.noah.perplayerkit.validation.Validator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Simple IoC (Inversion of Control) container for dependency injection.
 * Provides singleton and transient service registration and resolution.
 */
public class ServiceContainer {
    
    private final ConcurrentHashMap<Class<?>, ServiceRegistration<?>> services;
    private final ConcurrentHashMap<Class<?>, Object> singletonInstances;
    
    public ServiceContainer() {
        this.services = new ConcurrentHashMap<>();
        this.singletonInstances = new ConcurrentHashMap<>();
    }
    
    /**
     * Registers a singleton service.
     *
     * @param serviceClass the service interface/class
     * @param instance the singleton instance
     * @param <T> the service type
     * @return this container for method chaining
     */
    @NotNull
    public <T> ServiceContainer registerSingleton(@NotNull Class<T> serviceClass, @NotNull T instance) {
        Validator.requireNonNull(serviceClass, "serviceClass");
        Validator.requireNonNull(instance, "instance");
        
        services.put(serviceClass, new ServiceRegistration<>(instance, ServiceLifetime.SINGLETON));
        singletonInstances.put(serviceClass, instance);
        return this;
    }
    
    /**
     * Registers a singleton service with a factory method.
     *
     * @param serviceClass the service interface/class
     * @param factory the factory method to create the service
     * @param <T> the service type
     * @return this container for method chaining
     */
    @NotNull
    public <T> ServiceContainer registerSingleton(@NotNull Class<T> serviceClass, @NotNull Supplier<T> factory) {
        Validator.requireNonNull(serviceClass, "serviceClass");
        Validator.requireNonNull(factory, "factory");
        
        services.put(serviceClass, new ServiceRegistration<>(factory, ServiceLifetime.SINGLETON));
        return this;
    }
    
    /**
     * Registers a transient service (new instance each time).
     *
     * @param serviceClass the service interface/class
     * @param factory the factory method to create the service
     * @param <T> the service type
     * @return this container for method chaining
     */
    @NotNull
    public <T> ServiceContainer registerTransient(@NotNull Class<T> serviceClass, @NotNull Supplier<T> factory) {
        Validator.requireNonNull(serviceClass, "serviceClass");
        Validator.requireNonNull(factory, "factory");
        
        services.put(serviceClass, new ServiceRegistration<>(factory, ServiceLifetime.TRANSIENT));
        return this;
    }
    
    /**
     * Resolves a service from the container.
     *
     * @param serviceClass the service class to resolve
     * @param <T> the service type
     * @return the service instance
     * @throws ServiceNotFoundException if the service is not registered
     */
    @NotNull
    public <T> T resolve(@NotNull Class<T> serviceClass) {
        Validator.requireNonNull(serviceClass, "serviceClass");
        
        ServiceRegistration<?> registration = services.get(serviceClass);
        if (registration == null) {
            throw new ServiceNotFoundException("Service not registered: " + serviceClass.getName());
        }
        
        return resolveService(serviceClass, registration);
    }
    
    /**
     * Tries to resolve a service from the container.
     *
     * @param serviceClass the service class to resolve
     * @param <T> the service type
     * @return the service instance, or null if not registered
     */
    @Nullable
    public <T> T tryResolve(@NotNull Class<T> serviceClass) {
        try {
            return resolve(serviceClass);
        } catch (ServiceNotFoundException e) {
            return null;
        }
    }
    
    /**
     * Checks if a service is registered.
     *
     * @param serviceClass the service class to check
     * @return true if the service is registered
     */
    public boolean isRegistered(@NotNull Class<?> serviceClass) {
        return services.containsKey(serviceClass);
    }
    
    /**
     * Unregisters a service from the container.
     *
     * @param serviceClass the service class to unregister
     * @param <T> the service type
     * @return this container for method chaining
     */
    @NotNull
    public <T> ServiceContainer unregister(@NotNull Class<T> serviceClass) {
        services.remove(serviceClass);
        singletonInstances.remove(serviceClass);
        return this;
    }
    
    /**
     * Clears all registered services.
     */
    public void clear() {
        services.clear();
        singletonInstances.clear();
    }
    
    /**
     * Gets the number of registered services.
     *
     * @return the service count
     */
    public int getServiceCount() {
        return services.size();
    }
    
    /**
     * Resolves a service using its registration.
     */
    @SuppressWarnings("unchecked")
    private <T> T resolveService(@NotNull Class<T> serviceClass, @NotNull ServiceRegistration<?> registration) {
        if (registration.getLifetime() == ServiceLifetime.SINGLETON) {
            // Check if singleton instance already exists
            Object existingInstance = singletonInstances.get(serviceClass);
            if (existingInstance != null) {
                return (T) existingInstance;
            }
            
            // Create singleton instance
            T instance;
            if (registration.getInstance() != null) {
                instance = (T) registration.getInstance();
            } else {
                instance = (T) registration.getFactory().get();
            }
            
            singletonInstances.put(serviceClass, instance);
            return instance;
        } else {
            // Create transient instance
            return (T) registration.getFactory().get();
        }
    }
    
    /**
     * Service registration information.
     */
    private static final class ServiceRegistration<T> {
        private final T instance;
        private final Supplier<T> factory;
        private final ServiceLifetime lifetime;
        
        private ServiceRegistration(@NotNull T instance, @NotNull ServiceLifetime lifetime) {
            this.instance = instance;
            this.factory = null;
            this.lifetime = lifetime;
        }
        
        private ServiceRegistration(@NotNull Supplier<T> factory, @NotNull ServiceLifetime lifetime) {
            this.instance = null;
            this.factory = factory;
            this.lifetime = lifetime;
        }
        
        public T getInstance() { return instance; }
        public Supplier<T> getFactory() { return factory; }
        public ServiceLifetime getLifetime() { return lifetime; }
    }
    
    /**
     * Service lifetime enumeration.
     */
    public enum ServiceLifetime {
        SINGLETON,
        TRANSIENT
    }
    
    /**
     * Exception thrown when a service is not found in the container.
     */
    public static class ServiceNotFoundException extends RuntimeException {
        public ServiceNotFoundException(@NotNull String message) {
            super(message);
        }
    }
}