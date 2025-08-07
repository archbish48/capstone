package com.community.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@ImportAutoConfiguration(exclude = {
		org.springdoc.core.configuration.SpringDocConfiguration.class
})
class DemoApplicationTests {

}
