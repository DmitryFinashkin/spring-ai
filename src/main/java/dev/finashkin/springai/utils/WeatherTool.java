package dev.finashkin.springai.utils;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class WeatherTool {
    @Tool(description = "Provides weather information for a given location")
    public String weather(String city){
        //In prod, call a real weather API

        return switch(city.toLowerCase()) {

            case "london" -> "Rainy, 12 C";
            case "tokyo" -> "Sunny, 25 C";
            case "riga" -> "Cloudy, -2 C";
            default -> "Unknown city";
        };
    }

}
