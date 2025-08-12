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
package dev.noah.perplayerkit.gui2.data;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Advanced placeholder resolver that supports complex expressions and functions.
 * Handles both simple {placeholder} syntax and advanced {function:parameter} syntax.
 * 
 * Supported patterns:
 * - {player_name} - Simple data lookup
 * - {if:condition:true_value:false_value} - Conditional expressions
 * - {math:5+3} - Mathematical expressions
 * - {format:number:%.2f} - Formatting functions
 * - {time:yyyy-MM-dd} - Time formatting
 * - {upper:text} - Text transformation
 * - {color:&a&lGreen Text} - Color code translation
 */
public class PlaceholderResolver {
    
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([^}]+)}");
    private static final Pattern FUNCTION_PATTERN = Pattern.compile("([a-zA-Z_]+):(.+)");
    
    // Built-in function registry
    private static final Map<String, Function<String[], String>> FUNCTIONS = new HashMap<>();
    
    static {
        registerBuiltinFunctions();
    }
    
    /**
     * Resolve all placeholders in a text string
     */
    public static String resolve(String text, DataContext context) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        String result = text;
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(result);
        
        while (matcher.find()) {
            String placeholder = matcher.group(1);
            String value = resolvePlaceholder(placeholder, context);
            
            if (value != null) {
                result = result.replace("{" + placeholder + "}", value);
            }
        }
        
        // Apply color codes
        return ChatColor.translateAlternateColorCodes('&', result);
    }
    
    /**
     * Resolve a single placeholder
     */
    private static String resolvePlaceholder(String placeholder, DataContext context) {
        // Check for function syntax
        Matcher functionMatcher = FUNCTION_PATTERN.matcher(placeholder);
        if (functionMatcher.matches()) {
            String functionName = functionMatcher.group(1);
            String parameters = functionMatcher.group(2);
            
            return executeFunction(functionName, parameters, context);
        }
        
        // Simple data lookup
        Object value = context.get(placeholder);
        return value != null ? value.toString() : "{" + placeholder + "}";
    }
    
    /**
     * Execute a function with parameters
     */
    private static String executeFunction(String functionName, String parameters, DataContext context) {
        Function<String[], String> function = FUNCTIONS.get(functionName.toLowerCase());
        if (function == null) {
            return "{" + functionName + ":" + parameters + "}";
        }
        
        try {
            // Split parameters by colon, but allow escaping
            String[] params = splitParameters(parameters);
            
            // Resolve nested placeholders in parameters
            for (int i = 0; i < params.length; i++) {
                params[i] = resolve(params[i], context);
            }
            
            String result = function.apply(params);
            return result != null ? result : "";
            
        } catch (Exception e) {
            return "{ERROR:" + functionName + "}";
        }
    }
    
    /**
     * Split parameters by colon, handling escape sequences
     */
    private static String[] splitParameters(String parameters) {
        // Simple split for now - could be enhanced for escaped colons
        return parameters.split(":");
    }
    
    /**
     * Register built-in functions
     */
    private static void registerBuiltinFunctions() {
        // Conditional function: {if:condition:true_value:false_value}
        FUNCTIONS.put("if", params -> {
            if (params.length < 3) return "";
            
            boolean condition = parseBoolean(params[0]);
            return condition ? params[1] : params[2];
        });
        
        // Math function: {math:5+3}
        FUNCTIONS.put("math", params -> {
            if (params.length < 1) return "0";
            
            try {
                return String.valueOf(evaluateExpression(params[0]));
            } catch (Exception e) {
                return "ERROR";
            }
        });
        
        // Format function: {format:number:%.2f}
        FUNCTIONS.put("format", params -> {
            if (params.length < 2) return "";
            
            try {
                double number = Double.parseDouble(params[0]);
                String format = params[1];
                return String.format(format, number);
            } catch (Exception e) {
                return params[0];
            }
        });
        
        // Time function: {time:yyyy-MM-dd HH:mm:ss}
        FUNCTIONS.put("time", params -> {
            String pattern = params.length > 0 ? params[0] : "yyyy-MM-dd HH:mm:ss";
            
            try {
                SimpleDateFormat formatter = new SimpleDateFormat(pattern);
                return formatter.format(new Date());
            } catch (Exception e) {
                return new Date().toString();
            }
        });
        
        // Text transformation functions
        FUNCTIONS.put("upper", params -> params.length > 0 ? params[0].toUpperCase() : "");
        FUNCTIONS.put("lower", params -> params.length > 0 ? params[0].toLowerCase() : "");
        FUNCTIONS.put("capitalize", params -> {
            if (params.length < 1 || params[0].isEmpty()) return "";
            return params[0].substring(0, 1).toUpperCase() + params[0].substring(1).toLowerCase();
        });
        
        // Length function
        FUNCTIONS.put("length", params -> params.length > 0 ? String.valueOf(params[0].length()) : "0");
        
        // Substring function: {substring:text:start:end}
        FUNCTIONS.put("substring", params -> {
            if (params.length < 2) return "";
            
            try {
                String text = params[0];
                int start = Integer.parseInt(params[1]);
                int end = params.length > 2 ? Integer.parseInt(params[2]) : text.length();
                
                start = Math.max(0, Math.min(start, text.length()));
                end = Math.max(start, Math.min(end, text.length()));
                
                return text.substring(start, end);
            } catch (Exception e) {
                return params[0];
            }
        });
        
        // Default function: {default:value:fallback}
        FUNCTIONS.put("default", params -> {
            if (params.length < 2) return "";
            
            String value = params[0];
            String fallback = params[1];
            
            return (value == null || value.isEmpty() || value.startsWith("{")) ? fallback : value;
        });
        
        // Color function: {color:&a&lGreen Text}
        FUNCTIONS.put("color", params -> {
            if (params.length < 1) return "";
            return ChatColor.translateAlternateColorCodes('&', params[0]);
        });
        
        // Random function: {random:1:10} or {random:option1:option2:option3}
        FUNCTIONS.put("random", params -> {
            if (params.length < 1) return "";
            
            if (params.length == 2) {
                // Range: {random:1:10}
                try {
                    int min = Integer.parseInt(params[0]);
                    int max = Integer.parseInt(params[1]);
                    return String.valueOf((int) (Math.random() * (max - min + 1)) + min);
                } catch (NumberFormatException e) {
                    // Treat as options
                }
            }
            
            // Options: {random:option1:option2:option3}
            int index = (int) (Math.random() * params.length);
            return params[index];
        });
    }
    
    /**
     * Parse a string as a boolean
     */
    private static boolean parseBoolean(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        
        value = value.toLowerCase().trim();
        
        // Check for explicit boolean values
        if ("true".equals(value) || "yes".equals(value) || "1".equals(value)) {
            return true;
        }
        
        if ("false".equals(value) || "no".equals(value) || "0".equals(value)) {
            return false;
        }
        
        // Check for non-empty strings
        return !value.isEmpty() && !value.equals("{empty}");
    }
    
    /**
     * Evaluate a simple mathematical expression
     * This is a basic implementation - could be enhanced with a proper expression parser
     */
    private static double evaluateExpression(String expression) {
        // Very basic math evaluation - supports +, -, *, /
        expression = expression.replaceAll("\\s+", "");
        
        // Handle simple operations
        if (expression.contains("+")) {
            String[] parts = expression.split("\\+");
            double result = Double.parseDouble(parts[0]);
            for (int i = 1; i < parts.length; i++) {
                result += Double.parseDouble(parts[i]);
            }
            return result;
        }
        
        if (expression.contains("-") && !expression.startsWith("-")) {
            String[] parts = expression.split("-");
            double result = Double.parseDouble(parts[0]);
            for (int i = 1; i < parts.length; i++) {
                result -= Double.parseDouble(parts[i]);
            }
            return result;
        }
        
        if (expression.contains("*")) {
            String[] parts = expression.split("\\*");
            double result = Double.parseDouble(parts[0]);
            for (int i = 1; i < parts.length; i++) {
                result *= Double.parseDouble(parts[i]);
            }
            return result;
        }
        
        if (expression.contains("/")) {
            String[] parts = expression.split("/");
            double result = Double.parseDouble(parts[0]);
            for (int i = 1; i < parts.length; i++) {
                result /= Double.parseDouble(parts[i]);
            }
            return result;
        }
        
        // Single number
        return Double.parseDouble(expression);
    }
    
    /**
     * Register a custom function
     */
    public static void registerFunction(String name, Function<String[], String> function) {
        FUNCTIONS.put(name.toLowerCase(), function);
    }
    
    /**
     * Unregister a function
     */
    public static void unregisterFunction(String name) {
        FUNCTIONS.remove(name.toLowerCase());
    }
    
    /**
     * Check if a function is registered
     */
    public static boolean hasFunction(String name) {
        return FUNCTIONS.containsKey(name.toLowerCase());
    }
    
    /**
     * Get all registered function names
     */
    public static String[] getRegisteredFunctions() {
        return FUNCTIONS.keySet().toArray(new String[0]);
    }
}