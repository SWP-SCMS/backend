package com.scms.backend.course;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
class CourseImageResources implements WebMvcConfigurer {
    private final Path directory;
    CourseImageResources(@Value("${scms.course-images.directory:./data/course-images}") String directory) {
        this.directory = Path.of(directory).toAbsolutePath().normalize();
    }
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = directory.toUri().toString();
        registry.addResourceHandler("/course-images/**")
            .addResourceLocations(location.endsWith("/") ? location : location + "/");
    }
}
